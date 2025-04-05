package com.doan.services;

import com.doan.dto.*;
import com.doan.repository.Dang_Ky_Repository;
import com.doan.repository.Lich_Thi_Repository;
import com.doan.repository.Mon_Hoc_Repository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.doan.controller.Common;

import java.awt.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.sql.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class ExamSchedulerService {
	@Autowired
	private Mon_Hoc_Repository monHocRepository;
	@Autowired
	private Dang_Ky_Repository dangKyRepository;
	@Autowired
	private Lich_Thi_Repository lichThiRepository;
	@Autowired
	private Mon_Hoc_Service monHocService;

	private final ThreadLocalRandom random = ThreadLocalRandom.current();
	private long totalDays = 2L;
	private LocalDate dateFrom = LocalDate.now();
	private int populationSize = 200; // Increased population size
	private double crossoverRate = 0.85;
	private double mutationRate = 0.15;
	private int startHour = 9;
	private int endHour = 18;
	private int totalCourse = 100;
	private int maxExamPerDay = 5;
	// Time slots of 15 minutes each
	private List<LocalTime> timeSlots;
	private Map<String, List<String>> studentCourseMap = new HashMap<>();
	private List<Mon_Hoc> selected_course = new ArrayList<>();

	private List<LocalTime> generateTimeSlots(int startHour) {
		List<LocalTime> res = new ArrayList<>();
		LocalTime current = LocalTime.of(startHour, 0);
		List<Integer> slots = Arrays.asList(8, 10, 13, 15, 16);
		for (Integer s : slots) {
			if (current.getHour() > s) continue;
			if (s == 16) {
				res.add(LocalTime.of(16, 30));
			} else {
				res.add(LocalTime.of(s, 0));
			}
		}
		return res;
	}

	public List<Lich_Thi_DTO> generateExamSchedule(Lich_Thi_Option options) {
		System.out.println("Starting enhanced schedule generation");
		List<Mon_Hoc> courses = monHocService.getSubjectsWithUniqueNames(options.getSelectedSubjects());
		List<Dang_Ky> registrations = dangKyRepository.findDangKyByTenMonHocIn(options.getSelectedSubjects());
		System.out.println("Total course: " + courses.size());
		// Pre-process student registrations
		this.studentCourseMap = initializeStudentCourseMap(registrations);
		selected_course = courses;
		// Initialize parameters
		this.totalDays = ChronoUnit.DAYS.between(LocalDate.parse(options.getDayFrom()),
				LocalDate.parse(options.getDayTo())) + 1;
		int maxGenerations = options.getMaxGenerations();
		this.populationSize = options.getPopulationSize();
		this.crossoverRate = options.getCrossoverRate();
		this.mutationRate = options.getMutationRate();
		this.startHour = options.getHourFromInt();
		this.endHour = options.getHourToInt();
		this.dateFrom = LocalDate.parse(options.getDayFrom());
		this.totalCourse = courses.size();
		this.timeSlots = generateTimeSlots(this.startHour);
		this.maxExamPerDay = options.getMaxExamPerDay();
		List<Lich_Thi> bestSchedule = null;
		int bestFitness = Integer.MIN_VALUE;
		// Create thread pool
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<List<Lich_Thi>> population = generateInitialPopulation(courses);

		try {
			for (int generation = 0; generation < maxGenerations; generation++) {
				System.out.println("Generator: " + generation);
				List<Future<Integer>> fitnessValues = evaluatePopulation(population, executor);
				List<Integer> fitnessScores = collectFitnessScores(fitnessValues);

				int currentBestIndex = findBestSolution(fitnessScores);
				if (bestSchedule == null || fitnessScores.get(currentBestIndex) > bestFitness) {
					bestSchedule = new ArrayList<>(population.get(currentBestIndex));
					bestFitness = fitnessScores.get(currentBestIndex);
					log(generation, bestFitness, bestSchedule);
				}

				if (bestFitness >= 950) break; // Near-perfect solution found

				population = evolvePopulation(population, fitnessScores);
			}
			if (bestSchedule != null) {
				System.out.println(("Total course: " + courses.size()));
				System.out.println(("Total course in schedule: " + bestSchedule.size()));
			}
		} finally {
			executor.shutdown();
		}

		return saveAndConvertSchedule(bestSchedule);
	}

	private Map<String, List<String>> initializeStudentCourseMap(List<Dang_Ky> registrations) {
		Map<String, List<String>> res = new HashMap<>();
		res.clear();
		for (Dang_Ky reg : registrations) {
			res.computeIfAbsent(reg.ma_sinh_vien, k -> new ArrayList<>())
					.add(reg.tenMonHoc);
		}
		return res;
	}

	private List<List<Lich_Thi>> generateInitialPopulation(List<Mon_Hoc> courses) {
		List<List<Lich_Thi>> population = Collections.synchronizedList(new ArrayList<>());
		for (int i = 0; i < populationSize; i++) {
			List<Lich_Thi> schedule = generateConflictAwareSchedule(courses);
			population.add(schedule);
		}
		return population;
	}

	private List<Lich_Thi> generateConflictAwareSchedule(List<Mon_Hoc> courses) {
		List<Lich_Thi> schedule = new ArrayList<>();
		List<Mon_Hoc> unscheduledCourses = new ArrayList<>(courses);

		// Sort courses by duration (descending)
		unscheduledCourses.sort((a, b) -> Integer.compare(b.thoi_luong_thi, a.thoi_luong_thi));

		while (!unscheduledCourses.isEmpty()) {
			boolean scheduled = false;
			Mon_Hoc course = unscheduledCourses.get(0);
			List<Common.Pair<LocalDate, LocalTime>> availableSlots = new ArrayList<>();

			// Generate all possible time slots
			for (long dayOffset = 0; dayOffset < totalDays; dayOffset++) {
				LocalDate examDate = dateFrom.plusDays(dayOffset);
				for (LocalTime startTime : timeSlots) {
					Date sqlDate = Date.valueOf(examDate);
					Time sqlTime = Time.valueOf(startTime);

					// Check if slot has capacity
					if (countExamsInTimeSlot(schedule, sqlDate, sqlTime) < maxExamPerDay) {
						availableSlots.add(new Common.Pair<>(examDate, startTime));
					}
				}
			}

			// Shuffle available slots for randomization
			Collections.shuffle(availableSlots);

			// Try to schedule in available slots
			for (Common.Pair<LocalDate, LocalTime> slot : availableSlots) {
				Lich_Thi newExam = new Lich_Thi(
						course.tenMonHoc,
						Date.valueOf(slot.getKey()),
						Time.valueOf(slot.getValue()),
						course,
						"Room-" + (schedule.size() + 1)
				);

				if (isValidExamPlacement(newExam, schedule)) {
					schedule.add(newExam);
					unscheduledCourses.remove(0);
					scheduled = true;
					break;
				}
			}
			// If no valid slot found, try to force placement in least conflicting slot
			if (!scheduled) {
				LocalDate lastResortDate = dateFrom.plusDays(totalDays - 1);

				// Find the least conflicting time slot
				Lich_Thi bestExam = null;
				int minConflicts = Integer.MAX_VALUE;

				for (LocalTime time : timeSlots) {
					Lich_Thi candidateExam = new Lich_Thi(
							course.tenMonHoc,
							Date.valueOf(lastResortDate),
							Time.valueOf(time),
							course,
							"Room-" + (schedule.size() + 1)
					);

					int conflicts = countConflicts(candidateExam, schedule);
					if (conflicts < minConflicts) {
						minConflicts = conflicts;
						bestExam = candidateExam;
					}
				}

				if (bestExam != null) {
					schedule.add(bestExam);
					unscheduledCourses.remove(0);
				}
			}
		}
		return schedule;
	}
	private int countConflicts(Lich_Thi exam, List<Lich_Thi> schedule) {
		int conflicts = 0;
		LocalTime examStart = exam.getGio_thi().toLocalTime();
		LocalTime examEnd = examStart.plusMinutes(exam.getMonHoc().thoi_luong_thi);

		for (Lich_Thi existing : schedule) {
			if (existing.getNgay_thi().equals(exam.getNgay_thi())) {
				LocalTime existingStart = existing.getGio_thi().toLocalTime();
				LocalTime existingEnd = existingStart.plusMinutes(existing.getMonHoc().thoi_luong_thi);

				if (!(examEnd.isBefore(existingStart) || examStart.isAfter(existingEnd))) {
					conflicts++;
				}
			}
		}
		return conflicts;
	}
	private LocalTime getRandomValidTimeSlot(int duration) {
		List<LocalTime> validSlots = new ArrayList<>();

		for (LocalTime slot : timeSlots) {
			LocalTime endTime = slot.plusMinutes(duration);
			if (!endTime.isAfter(LocalTime.of(endHour, 0)) &&
					!isOverlappingLunchBreak(slot, endTime)) {
				validSlots.add(slot);
			}
		}

		return validSlots.isEmpty() ? null :
				validSlots.get(random.nextInt(validSlots.size()));
	}

	private boolean isOverlappingLunchBreak(LocalTime start, LocalTime end) {
		LocalTime lunchStart = LocalTime.of(12, 0);
		LocalTime lunchEnd = LocalTime.of(13, 0);
		return !(end.isBefore(lunchStart) || start.isAfter(lunchEnd));
	}

	private boolean isValidExamPlacement(Lich_Thi newExam, List<Lich_Thi> schedule) {
		LocalTime newStartTime = newExam.getGio_thi().toLocalTime();
		LocalTime newEndTime = newStartTime.plusMinutes(newExam.getMonHoc().thoi_luong_thi);

		// Check time slot constraint
		int examCountInSlot = countExamsInTimeSlot(schedule, newExam.getNgay_thi(), newExam.getGio_thi());
		if (examCountInSlot >= maxExamPerDay) {
			return false;
		}

		// Check for student conflicts
		for (Lich_Thi existingExam : schedule) {
			if (existingExam.getNgay_thi().equals(newExam.getNgay_thi())) {
				LocalTime existingStartTime = existingExam.getGio_thi().toLocalTime();
				LocalTime existingEndTime = existingStartTime
						.plusMinutes(existingExam.getMonHoc().thoi_luong_thi);

				// Time overlap check
				if (!(newEndTime.isBefore(existingStartTime) ||
						newStartTime.isAfter(existingEndTime))) {
					// Check if any student is registered for both exams
					for (Map.Entry<String, List<String>> entry : studentCourseMap.entrySet()) {
						if (entry.getValue().contains(newExam.getTen_mon_hoc()) &&
								entry.getValue().contains(existingExam.getTen_mon_hoc())) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private int calculateFitness(List<Lich_Thi> schedule) {
		int fitness = 1000;

		if (!validateSchedule(schedule, selected_course)) {
			return -1;  // Invalid schedule gets zero fitness
		}

		Map<String, Map<Time, Integer>> timeSlotCounts = new HashMap<>();
		Set<String> conflictPairs = new HashSet<>();

		if (schedule.size() != totalCourse) return 0;

		// Count exams per time slot
//		// Check time slot violations
		for (int i = 0; i < schedule.size(); i++) {
			Lich_Thi exam1 = schedule.get(i);
			LocalTime exam1EndTime = exam1.getGio_thi().toLocalTime()
					.plusMinutes(exam1.getMonHoc().thoi_luong_thi);

			// Penalty for end time violations
			if (exam1EndTime.isAfter(LocalTime.of(endHour, 0))) {
				fitness -= 100;
			}

			// Count exams per day and apply penalty for exceeding maxExamPerDay
			String dateKey = exam1.getNgay_thi().toString();
			timeSlotCounts.computeIfAbsent(dateKey, k -> new HashMap<>())
					.merge(exam1.getGio_thi(), 1, Integer::sum);


			// Check for conflicts with other exams
			for (int j = i + 1; j < schedule.size(); j++) {
				Lich_Thi exam2 = schedule.get(j);

				if (exam1.getNgay_thi().equals(exam2.getNgay_thi())) {
					Lich_Thi first_exam = exam1.getGio_thi().before(exam2.getGio_thi()) ? exam1 : exam2;
					Lich_Thi second_exam = exam1.getGio_thi().before(exam2.getGio_thi()) ? exam2 : exam1;
					LocalTime first_exam_end = first_exam.getGio_thi().toLocalTime().plusMinutes(first_exam.getMonHoc().thoi_luong_thi);
					LocalTime second_exam_start = second_exam.getGio_thi().toLocalTime();

					if (first_exam_end.isAfter(second_exam_start)) {
						// Check for student conflicts
						for (List<String> studentCourses : studentCourseMap.values()) {
							if (studentCourses.contains(exam1.getTen_mon_hoc()) &&
									studentCourses.contains(exam2.getTen_mon_hoc())) {
								String conflictKey = exam1.getTen_mon_hoc() + "-" + exam2.getTen_mon_hoc();
								if (conflictPairs.add(conflictKey)) {
									fitness -= 200; // Severe penalty for student conflicts
								}
							}
						}
					}
				}
			}
		}

		for (Map<Time, Integer> slotCounts : timeSlotCounts.values()) {
			for (int count : slotCounts.values()) {
				if (count > maxExamPerDay) {
					fitness -= 200 * (count - maxExamPerDay); // Severe penalty for exceeding time slot limit
				}
			}
		}

		return Math.max(0, fitness);
	}

	private List<List<Lich_Thi>> evolvePopulation(List<List<Lich_Thi>> population,
	                                              List<Integer> fitnessScores) {
		List<List<Lich_Thi>> newPopulation = new ArrayList<>();

		// Elitism - keep the best 10% of solutions
		int eliteCount = populationSize / 10;
		List<Integer> sortedIndices = IntStream.range(0, fitnessScores.size())
				.boxed()
				.sorted((i, j) -> fitnessScores.get(j).compareTo(fitnessScores.get(i)))
				.collect(Collectors.toList());

		for (int i = 0; i < eliteCount; i++) {
			newPopulation.add(new ArrayList<>(population.get(sortedIndices.get(i))));
		}

		// Generate rest of the population
		while (newPopulation.size() < populationSize) {
			List<Lich_Thi> parent1 = tournamentSelection(population, fitnessScores);
			List<Lich_Thi> parent2 = tournamentSelection(population, fitnessScores);
			List<List<Lich_Thi>> offspring = crossover(parent1, parent2);
			newPopulation.add(mutate(offspring.get(0)));
			if (newPopulation.size() < populationSize) {
				newPopulation.add(mutate(offspring.get(1)));
			}
		}

		return newPopulation;
	}

	private List<Lich_Thi> tournamentSelection(List<List<Lich_Thi>> population,
	                                           List<Integer> fitnessScores) {
		int tournamentSize = 5;
		int bestIndex = -1;
		int bestFitness = Integer.MIN_VALUE;

		for (int i = 0; i < tournamentSize; i++) {
			int index = random.nextInt(population.size());
			if (fitnessScores.get(index) > bestFitness) {
				bestFitness = fitnessScores.get(index);
				bestIndex = index;
			}
		}

		return new ArrayList<>(population.get(bestIndex));
	}

	private List<List<Lich_Thi>> crossover(List<Lich_Thi> parent1, List<Lich_Thi> parent2) {
		if (random.nextDouble() > crossoverRate) {
			return Arrays.asList(new ArrayList<>(parent1), new ArrayList<>(parent2));
		}

		Map<String, Lich_Thi> courseMap1 = new HashMap<>();
		Map<String, Lich_Thi> courseMap2 = new HashMap<>();

		// Create maps for quick lookup
		parent1.forEach(exam -> courseMap1.put(exam.getTen_mon_hoc(), exam));
		parent2.forEach(exam -> courseMap2.put(exam.getTen_mon_hoc(), exam));

		List<Lich_Thi> child1 = new ArrayList<>();
		List<Lich_Thi> child2 = new ArrayList<>();

		Set<String> processed1 = new HashSet<>();
		Set<String> processed2 = new HashSet<>();

		// Crossover point
		int crossPoint = random.nextInt(parent1.size());

		// First part from parent 1
		for (int i = 0; i < crossPoint; i++) {
			Lich_Thi exam1 = parent1.get(i);
			if (!processed1.contains(exam1.getTen_mon_hoc())) {
				child1.add(new Lich_Thi(exam1));
				processed1.add(exam1.getTen_mon_hoc());
			}
		}

		// Second part from parent 2
		for (int i = crossPoint; i < parent2.size(); i++) {
			Lich_Thi exam2 = parent2.get(i);
			if (!processed1.contains(exam2.getTen_mon_hoc())) {
				child1.add(new Lich_Thi(exam2));
				processed1.add(exam2.getTen_mon_hoc());
			}
		}

		// Add missing courses to child1
		courseMap1.forEach((courseId, exam) -> {
			if (!processed1.contains(courseId)) {
				child1.add(new Lich_Thi(exam));
				processed1.add(courseId);
			}
		});

		// Repeat for child2 with reversed parents
		// First part from parent 2
		for (int i = 0; i < crossPoint; i++) {
			Lich_Thi exam2 = parent2.get(i);
			if (!processed2.contains(exam2.getTen_mon_hoc())) {
				child2.add(new Lich_Thi(exam2));
				processed2.add(exam2.getTen_mon_hoc());
			}
		}

		// Second part from parent 1
		for (int i = crossPoint; i < parent1.size(); i++) {
			Lich_Thi exam1 = parent1.get(i);
			if (!processed2.contains(exam1.getTen_mon_hoc())) {
				child2.add(new Lich_Thi(exam1));
				processed2.add(exam1.getTen_mon_hoc());
			}
		}

		// Add missing courses to child2
		courseMap2.forEach((courseId, exam) -> {
			if (!processed2.contains(courseId)) {
				child2.add(new Lich_Thi(exam));
				processed2.add(courseId);
			}
		});

		return Arrays.asList(child1, child2);
	}

	// Check if all course are present
	private List<String> checkNotExists(List<Lich_Thi> schedule, List<Mon_Hoc> allCourses) {
		Set<String> requiredCourses = allCourses.stream()
				.map(c -> c.tenMonHoc)
				.collect(Collectors.toSet());

		Set<String> scheduledCourses = schedule.stream()
				.map(Lich_Thi::getTen_mon_hoc)
				.collect(Collectors.toSet());

		return requiredCourses.stream()
				.filter(course -> !scheduledCourses.contains(course))
				.collect(Collectors.toList());
	}

	private Set<String> checkDuplicate(List<Lich_Thi> schedule) {
		return schedule.stream()
				.map(Lich_Thi::getTen_mon_hoc)
				.filter(course -> Collections.frequency(
						schedule.stream()
								.map(Lich_Thi::getTen_mon_hoc)
								.collect(Collectors.toList()),
						course) > 1)
				.collect(Collectors.toSet());
	}

	private boolean validateSchedule(List<Lich_Thi> schedule, List<Mon_Hoc> allCourses) {

		List<String> notExistsCourse = checkNotExists(schedule, allCourses);
		Set<String> duplicateCourse = checkDuplicate(schedule);

//		if (!notExistsCourse.isEmpty() || !duplicateCourse.isEmpty()) {
//			System.out.println("Schedule validation failed:");
//			System.out.println("Missing courses: " + notExistsCourse);
//			System.out.println("Duplicate courses: " + duplicateCourse);
//		}

		return notExistsCourse.isEmpty() && duplicateCourse.isEmpty();
	}
	private void ensureAllCoursesPresent(List<Lich_Thi> child, List<Lich_Thi> parent1, List<Lich_Thi> parent2) {
		Set<String> coursesInChild = child.stream()
				.filter(Objects::nonNull)
				.map(Lich_Thi::getTen_mon_hoc)
				.collect(Collectors.toSet());

		Set<String> allCourses = Stream.concat(
						parent1.stream().map(Lich_Thi::getTen_mon_hoc),
						parent2.stream().map(Lich_Thi::getTen_mon_hoc))
				.collect(Collectors.toSet());

		// Add missing courses to the child
		for (String course : allCourses) {
			if (!coursesInChild.contains(course)) {
				// Find the exam for the missing course from either parent
				Lich_Thi missingExam = Stream.concat(parent1.stream(), parent2.stream())
						.filter(exam -> exam.getTen_mon_hoc().equals(course))
						.findFirst()
						.orElse(null);

				if (missingExam != null) {
					// Find the first available null position in the child
					int index = IntStream.range(0, child.size())
							.filter(i -> child.get(i) == null)
							.findFirst()
							.orElse(-1);

					if (index != -1) {
						child.set(index, new Lich_Thi(missingExam));
					}
				}
			}
		}
	}

	private List<Lich_Thi> mutate(List<Lich_Thi> schedule) {
		if (random.nextDouble() > mutationRate) {
			return schedule;
		}

		List<Lich_Thi> mutated = new ArrayList<>(schedule);
		int mutations = random.nextInt(3) + 1; // 1-3 mutations

		for (int i = 0; i < mutations; i++) {
			int index = random.nextInt(schedule.size());
			Lich_Thi exam = mutated.get(index);

			// Apply one of several mutation operations
			switch (random.nextInt(3)) {
				case 0: // Change date
					long newDay = random.nextLong(totalDays);
					exam = new Lich_Thi(
							exam.getTen_mon_hoc(),
							Date.valueOf(this.dateFrom.plusDays(newDay)),
							exam.getGio_thi(),
							exam.getMonHoc(),
							exam.getPhong_thi()
					);
					break;

				case 1: // Change time
					LocalTime newTime = getRandomValidTimeSlot(exam.getMonHoc().thoi_luong_thi);
					if (newTime != null) {
						exam = new Lich_Thi(
								exam.getTen_mon_hoc(),
								exam.getNgay_thi(),
								Time.valueOf(newTime),
								exam.getMonHoc(),
								exam.getPhong_thi()
						);
					}
					break;

//				case 2: // Swap with another exam
//					int swapIndex = random.nextInt(schedule.size());
//					if (swapIndex != index) {
//						Lich_Thi temp = mutated.get(swapIndex);
//						mutated.set(swapIndex, mutated.get(index));
//						mutated.set(index, temp);
//					}
//					break;
			}

			mutated.set(index, exam);
		}

		// Ensure all courses are still present after mutation
		ensureAllCoursesPresent(mutated, schedule, schedule);

		return mutated;
	}

	private List<Lich_Thi_DTO> saveAndConvertSchedule(List<Lich_Thi> schedule) {
		List<Lich_Thi_DTO> result = new ArrayList<>();
		for (Lich_Thi exam : schedule) {
			lichThiRepository.save(exam);
			result.add(new Lich_Thi_DTO(exam));
		}
		return result;
	}
	private List<Future<Integer>> evaluatePopulation(List<List<Lich_Thi>> population, ExecutorService executor) {
		List<Future<Integer>> fitnessValues = new ArrayList<>();

		// Submit fitness calculation tasks for each schedule in the population
		for (List<Lich_Thi> schedule : population) {
			Future<Integer> future = executor.submit(() -> calculateFitness(schedule));
			fitnessValues.add(future);
		}

		return fitnessValues;
	}

	private List<Integer> collectFitnessScores(List<Future<Integer>> fitnessValues) {
		List<Integer> fitnessScores = new ArrayList<>();

		for (Future<Integer> future : fitnessValues) {
			try {
				fitnessScores.add(future.get(5, TimeUnit.SECONDS)); // Timeout after 5 seconds
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				// If there's an error or timeout, assign a very low fitness score
				fitnessScores.add(0);
				e.printStackTrace();
				System.err.println("Error collecting fitness score: " + e.getMessage());
			}
		}

		return fitnessScores;
	}

	private int countExamsInTimeSlot(List<Lich_Thi> schedule, Date date, Time time) {
		return (int) schedule.stream()
				.filter(exam -> exam.getNgay_thi().equals(date) &&
						exam.getGio_thi().equals(time))
				.count();
	}
	private int findBestSolution(List<Integer> fitnessScores) {
		int bestIndex = 0;
		int bestFitness = fitnessScores.get(0);

		// Find the index of the highest fitness score
		for (int i = 1; i < fitnessScores.size(); i++) {
			if (fitnessScores.get(i) > bestFitness) {
				bestFitness = fitnessScores.get(i);
				bestIndex = i;
			}
		}

		// Optional: Log the best fitness score found
		if (bestFitness % 50 == 0) { // Log every 50 points of improvement
			System.out.printf("Found solution with fitness: %d\n", bestFitness);
		}

		return bestIndex;
	}
	public String evaluate(List<Lich_Thi_DTO> schedule, Lich_Thi_Option option) {
		StringBuilder result = new StringBuilder();
		List<Dang_Ky> all_regis = dangKyRepository.findAll();
		List<Mon_Hoc> all_course = monHocService.getSubjectsWithUniqueNames(option.getSelectedSubjects());
		List<Lich_Thi> schedule_org = schedule.stream().map(e -> {
			return new Lich_Thi(
					e.getTen_mon_hoc(),
					e.getNgay_thi(),
					e.getGio_thi(),
					monHocRepository.findByMaMonHoc(e.getTen_mon_hoc()),
					e.getPhong_thi());
		}).collect(Collectors.toList());
		List<String> notExistsCourse = checkNotExists(schedule_org, all_course);
		if (!notExistsCourse.isEmpty()) {
			result.append("Not exists course: " + notExistsCourse).append("\n");
		}
		Set<String> duplicateCourse = checkDuplicate(schedule_org);
		if (!duplicateCourse.isEmpty()) {
			result.append("Duplicate course: " + duplicateCourse).append("\n");
		}

		Map<String, List<Lich_Thi_DTO>> studentExamsMap = new HashMap<>();
		for (Dang_Ky registration : all_regis) {
			studentExamsMap.putIfAbsent(registration.ma_sinh_vien, new ArrayList<>());
		}

		// Group exams by student
		for (Lich_Thi_DTO lt : schedule) {
			for (Dang_Ky registration : all_regis) {
				if (registration.maMonHoc.equals(lt.getTen_mon_hoc())) {
					studentExamsMap.get(registration.ma_sinh_vien).add(lt);
				}
			}
		}
		Map<String, List<String>> sCourseMap = initializeStudentCourseMap(all_regis);
		Set<String> conflictPairs = new HashSet<>();
		for (int i = 0; i < schedule.size(); i++) {
			Lich_Thi_DTO exam1 = schedule.get(i);
			for (int j = i + 1; j < schedule.size(); j++) {
				Lich_Thi_DTO exam2 = schedule.get(j);
				if (exam1.getNgay_thi().equals(exam2.getNgay_thi())) {
					Lich_Thi_DTO first_exam = exam1.getGio_thi().before(exam2.getGio_thi()) ? exam1 : exam2;
					Lich_Thi_DTO second_exam = exam1.getGio_thi().before(exam2.getGio_thi()) ? exam2 : exam1;
					LocalTime first_exam_end = first_exam.getGio_thi().toLocalTime().plusMinutes(first_exam.getThoi_luong_thi());
					LocalTime second_exam_start = second_exam.getGio_thi().toLocalTime();

					if (first_exam_end.isAfter(second_exam_start)) {
						// Check for student conflicts
						for (List<String> studentCourses : sCourseMap.values()) {
							if (studentCourses.contains(exam1.getTen_mon_hoc()) &&
									studentCourses.contains(exam2.getTen_mon_hoc())) {
								String conflictKey = exam1.getTen_mon_hoc() + "-" + exam2.getTen_mon_hoc();
								if (conflictPairs.add(conflictKey)) {
									result.append(String.format("Conflict subject %s with %s\n", exam1.getTen_mon_hoc(), exam2.getTen_mon_hoc()));
								}
							}
						}
					}
				}
			}
		}
		return result.toString();
	}

	// Helper method to combine Date (ngay_thi) and Time (gio_thi) into a full Timestamp
	private Timestamp combineDateAndTime(java.sql.Date date, java.sql.Time time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);  // Use java.sql.Date as Date
		calendar.set(Calendar.HOUR_OF_DAY, time.getHours());
		calendar.set(Calendar.MINUTE, time.getMinutes());
		calendar.set(Calendar.SECOND, time.getSeconds());
		return new Timestamp(calendar.getTimeInMillis());  // Returns java.sql.Timestamp
	}

	// Enhanced version with statistics tracking
	@Getter
	private static class PopulationStatistics {
		private int bestFitness;
		private int worstFitness;
		private double averageFitness;
		private int bestIndex;

		public PopulationStatistics(List<Integer> fitnessScores) {
			this.bestFitness = Integer.MIN_VALUE;
			this.worstFitness = Integer.MAX_VALUE;
			long sum = 0;

			for (int i = 0; i < fitnessScores.size(); i++) {
				int fitness = fitnessScores.get(i);
				sum += fitness;

				if (fitness > bestFitness) {
					bestFitness = fitness;
					bestIndex = i;
				}
				if (fitness < worstFitness) {
					worstFitness = fitness;
				}
			}

			this.averageFitness = (double) sum / fitnessScores.size();
		}

		public void logStatistics(int generation) {
			System.out.printf("Generation %d stats:%n", generation);
			System.out.printf("Best Fitness: %d%n", bestFitness);
			System.out.printf("Worst Fitness: %d%n", worstFitness);
			System.out.printf("Average Fitness: %.2f%n", averageFitness);
			System.out.println("--------------------");
		}
	}

	// Constants for log levels
	private enum LogLevel {
		BASIC,    // Basic information
		DETAILED, // Detailed information
		DEBUG     // Debug information
	}

	private LogLevel currentLogLevel = LogLevel.BASIC;
	private int logInterval = 10; // Log every 10 generations
	private Map<Integer, Integer> fitnessHistory = new HashMap<>();
	private int bestFitnessSoFar = Integer.MIN_VALUE;

	public void log(int generation, int fitness, List<Lich_Thi> schedule) {
		// Update fitness history
		fitnessHistory.put(generation, fitness);
		boolean isImprovement = fitness > bestFitnessSoFar;
		if (isImprovement) {
			bestFitnessSoFar = fitness;
		}

		// Determine if we should log this generation
		boolean shouldLog = generation % logInterval == 0 || isImprovement;
		if (!shouldLog) return;

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("\n=== Generation ").append(generation)
				.append(" === Fitness: ").append(fitness).append(" ===\n");

		// Basic logging - always shown
		if (currentLogLevel != LogLevel.BASIC) {
			logMessage.append(String.format("Time: %s\n",
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)));
			logMessage.append(String.format("Best fitness so far: %d\n", bestFitnessSoFar));
		}

		// Detailed logging
		if (currentLogLevel == LogLevel.DETAILED || currentLogLevel == LogLevel.DEBUG) {
			logMessage.append("\nSchedule Details:\n");
			Map<Date, List<Lich_Thi>> examsByDate = schedule.stream()
					.collect(Collectors.groupingBy(Lich_Thi::getNgay_thi));

			examsByDate.forEach((date, exams) -> {
				logMessage.append("\nDate: ").append(date).append("\n");
				exams.stream()
						.sorted(Comparator.comparing(exam -> exam.getGio_thi().toLocalTime()))
						.forEach(exam -> {
							LocalTime endTime = exam.getGio_thi().toLocalTime()
									.plusMinutes(exam.getMonHoc().thoi_luong_thi);
							logMessage.append(String.format("  %s - %s to %s (%d minutes) - Room %s\n",
									exam.getMonHoc().tenMonHoc,
									exam.getGio_thi().toLocalTime(),
									endTime,
									exam.getMonHoc().thoi_luong_thi,
									exam.getPhong_thi()));
						});
			});
		}

		// Debug logging
		if (currentLogLevel == LogLevel.DEBUG) {
			logMessage.append("\nDetailed Analysis:\n");
			analyzeScheduleConflicts(schedule, logMessage);
			analyzeResourceUtilization(schedule, logMessage);
			logFitnessProgression(generation, logMessage);
		}

		// Print the log message
		System.out.println(logMessage.toString());
	}

	private void analyzeScheduleConflicts(List<Lich_Thi> schedule, StringBuilder logMessage) {
		Map<String, List<Lich_Thi>> examsByDay = new HashMap<>();
		int totalConflicts = 0;

		// Group exams by day
		for (Lich_Thi exam : schedule) {
			String dateKey = exam.getNgay_thi().toString();
			examsByDay.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(exam);
		}

		logMessage.append("\nConflict Analysis:\n");

		// Check for conflicts on each day
		for (Map.Entry<String, List<Lich_Thi>> entry : examsByDay.entrySet()) {
			List<Lich_Thi> dailyExams = entry.getValue();
			for (int i = 0; i < dailyExams.size(); i++) {
				for (int j = i + 1; j < dailyExams.size(); j++) {
					if (hasTimeConflict(dailyExams.get(i), dailyExams.get(j))) {
						totalConflicts++;
						logMessage.append(String.format("  Conflict found: %s and %s on %s\n",
								dailyExams.get(i).getMonHoc().tenMonHoc,
								dailyExams.get(j).getMonHoc().tenMonHoc,
								entry.getKey()));
					}
				}
			}
		}

		logMessage.append(String.format("Total conflicts: %d\n", totalConflicts));
	}

	private boolean hasTimeConflict(Lich_Thi exam1, Lich_Thi exam2) {
		LocalTime start1 = exam1.getGio_thi().toLocalTime();
		LocalTime end1 = start1.plusMinutes(exam1.getMonHoc().thoi_luong_thi);
		LocalTime start2 = exam2.getGio_thi().toLocalTime();
		LocalTime end2 = start2.plusMinutes(exam2.getMonHoc().thoi_luong_thi);

		return !(end1.isBefore(start2) || start1.isAfter(end2));
	}

	private void analyzeResourceUtilization(List<Lich_Thi> schedule, StringBuilder logMessage) {
		Map<String, Integer> roomUsage = new HashMap<>();
		Map<Date, Integer> dateUsage = new HashMap<>();

		for (Lich_Thi exam : schedule) {
			roomUsage.merge(exam.getPhong_thi(), 1, Integer::sum);
			dateUsage.merge(exam.getNgay_thi(), 1, Integer::sum);
		}

		logMessage.append("\nResource Utilization:\n");
		logMessage.append("Room Usage:\n");
		roomUsage.forEach((room, count) ->
				logMessage.append(String.format("  %s: %d exams\n", room, count)));

		logMessage.append("Date Usage:\n");
		dateUsage.forEach((date, count) ->
				logMessage.append(String.format("  %s: %d exams\n", date, count)));
	}

	private void logFitnessProgression(int generation, StringBuilder logMessage) {
		logMessage.append("\nFitness Progression:\n");

		// Calculate improvement rate
		if (generation >= logInterval) {
			int previousFitness = fitnessHistory.getOrDefault(generation - logInterval, 0);
			int currentFitness = fitnessHistory.get(generation);
			double improvementRate = ((double)(currentFitness - previousFitness) / previousFitness) * 100;

			logMessage.append(String.format("Improvement rate: %.2f%%\n", improvementRate));
		}

		// Show fitness trend
		int windowSize = Math.min(5, fitnessHistory.size());
		logMessage.append("Recent fitness trend: ");
		fitnessHistory.entrySet().stream()
				.sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
				.limit(windowSize)
				.forEach(entry -> logMessage.append(entry.getValue()).append(" → "));
		logMessage.setLength(logMessage.length() - 2); // Remove last arrow
		logMessage.append("\n");
	}

	// Utility method to set log level
	public void setLogLevel(LogLevel level) {
		this.currentLogLevel = level;
	}
}