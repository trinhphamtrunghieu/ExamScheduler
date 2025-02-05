package com.doan.services;

import com.doan.dto.*;
import com.doan.repository.Dang_Ky_Repository;
import com.doan.repository.Lich_Thi_Repository;
import com.doan.repository.Mon_Hoc_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

	private final ThreadLocalRandom random = ThreadLocalRandom.current();
	private long totalDays = 2L;
	private LocalDate dateFrom = LocalDate.now();
	private int populationSize = 200; // Increased population size
	private double crossoverRate = 0.85;
	private double mutationRate = 0.15;
	private int maxGenerations = 1000;
	private int startHour = 9;
	private int endHour = 18;
	private int totalCourse = 100;

	// Time slots of 15 minutes each
	private final List<LocalTime> timeSlots = generateTimeSlots();
	private Map<String, List<String>> studentCourseMap = new HashMap<>();

	private List<LocalTime> generateTimeSlots() {
		List<LocalTime> slots = new ArrayList<>();
		LocalTime current = LocalTime.of(startHour, 0);
		while (!current.isAfter(LocalTime.of(endHour, 0))) {
			if (!(current.getHour() == 12)) { // Skip lunch hour
				slots.add(current);
			}
			current = current.plusMinutes(15);
		}
		return slots;
	}

	public List<Lich_Thi_DTO> generateExamSchedule(Lich_Thi_Option options) {
		System.out.println("Starting enhanced schedule generation");
		List<Mon_Hoc> courses = monHocRepository.findByMaMonHocIn(options.getSelectedSubjects());
		List<Dang_Ky> registrations = dangKyRepository.findDangKyByMaMonHocIn(options.getSelectedSubjects());

		// Pre-process student registrations
		initializeStudentCourseMap(registrations);

		// Initialize parameters
		this.totalDays = ChronoUnit.DAYS.between(LocalDate.parse(options.getDayFrom()),
				LocalDate.parse(options.getDayTo())) + 1;
		this.maxGenerations = options.getMaxGenerations();
		this.populationSize = options.getPopulationSize();
		this.crossoverRate = options.getCrossoverRate();
		this.mutationRate = options.getMutationRate();
		this.startHour = options.getHourFromInt();
		this.endHour = options.getHourToInt();
		this.dateFrom = LocalDate.parse(options.getDayFrom());
		this.totalCourse = courses.size();
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
		} finally {
			executor.shutdown();
		}
		System.out.println(("Total course: " + courses.size()));
		System.out.println(("Total course in schedule: " + bestSchedule.size()));

		return saveAndConvertSchedule(bestSchedule);
	}

	private void initializeStudentCourseMap(List<Dang_Ky> registrations) {
		studentCourseMap.clear();
		for (Dang_Ky reg : registrations) {
			studentCourseMap.computeIfAbsent(reg.ma_sinh_vien, k -> new ArrayList<>())
					.add(reg.maMonHoc);
		}
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
		Set<String> scheduledSubjects = new HashSet<>(); // Track scheduled subjects
		List<Mon_Hoc> shuffledCourses = new ArrayList<>(courses);
		Collections.shuffle(shuffledCourses);

		// Ensure every course is scheduled at least once
		for (Mon_Hoc course : shuffledCourses) {
			boolean validSlotFound = false;
			int attempts = 0;

			while (!validSlotFound && attempts < 100) {
				long dayOffset = random.nextLong(totalDays);
				LocalDate examDate = dateFrom.plusDays(dayOffset);
				LocalTime startTime = getRandomValidTimeSlot(course.thoi_luong_thi);

				if (startTime != null) {
					Lich_Thi newExam = new Lich_Thi(
							course.maMonHoc,
							Date.valueOf(examDate),
							Time.valueOf(startTime),
							course,
							"Room-" + (schedule.size() + 1)
					);

					if (isValidExamPlacement(newExam, schedule)) {
						schedule.add(newExam);
						scheduledSubjects.add(course.maMonHoc); // Mark as scheduled
						validSlotFound = true;
					}
				}
				attempts++;
			}

			// If no valid slot is found, assign a fallback slot
			if (!validSlotFound) {
				System.out.println("Warning: Could not find a valid slot for " + course.maMonHoc + ", assigning fallback slot.");
				LocalDate fallbackDate = dateFrom.plusDays(random.nextLong(totalDays));
				LocalTime fallbackTime = getRandomValidTimeSlot(course.thoi_luong_thi);
				if (fallbackTime == null) fallbackTime = timeSlots.get(random.nextInt(timeSlots.size())); // Pick random time if needed

				Lich_Thi fallbackExam = new Lich_Thi(
						course.maMonHoc,
						Date.valueOf(fallbackDate),
						Time.valueOf(fallbackTime),
						course,
						"Room-" + (schedule.size() + 1)
				);
				schedule.add(fallbackExam);
				scheduledSubjects.add(course.maMonHoc);
			}
		}

		System.out.println("Final schedule size: " + schedule.size());
		return schedule;
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
						if (entry.getValue().contains(newExam.getMa_mon_hoc()) &&
								entry.getValue().contains(existingExam.getMa_mon_hoc())) {
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
		Map<String, Integer> dailyExamCount = new HashMap<>();
		Set<String> conflictPairs = new HashSet<>();
		if (schedule.size() != totalCourse) return 0;
		for (int i = 0; i < schedule.size(); i++) {
			Lich_Thi exam1 = schedule.get(i);
			LocalTime exam1EndTime = exam1.getGio_thi().toLocalTime()
					.plusMinutes(exam1.getMonHoc().thoi_luong_thi);

			// Penalty for end time violations
			if (exam1EndTime.isAfter(LocalTime.of(endHour, 0))) {
				fitness -= 100;
			}

			// Count exams per day
			String dateKey = exam1.getNgay_thi().toString();
			dailyExamCount.merge(dateKey, 1, Integer::sum);

			// Check for conflicts with other exams
			for (int j = i + 1; j < schedule.size(); j++) {
				Lich_Thi exam2 = schedule.get(j);

				if (exam1.getNgay_thi().equals(exam2.getNgay_thi())) {
					LocalTime exam2StartTime = exam2.getGio_thi().toLocalTime();

					if (exam1EndTime.isAfter(exam2StartTime)) {
						// Check for student conflicts
						for (List<String> studentCourses : studentCourseMap.values()) {
							if (studentCourses.contains(exam1.getMa_mon_hoc()) &&
									studentCourses.contains(exam2.getMa_mon_hoc())) {
								String conflictKey = exam1.getMa_mon_hoc() + "-" + exam2.getMa_mon_hoc();
								if (conflictPairs.add(conflictKey)) {
									fitness -= 200; // Severe penalty for student conflicts
								}
							}
						}
					}
				}
			}
		}

		// Penalty for uneven distribution of exams across days
		int maxExamsPerDay = Collections.max(dailyExamCount.values());
		int minExamsPerDay = Collections.min(dailyExamCount.values());
		fitness -= (maxExamsPerDay - minExamsPerDay) * 20;

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

		int size = Math.min(parent1.size(), parent2.size()); // Ensure valid index range

		List<Lich_Thi> child1 = new ArrayList<>(Collections.nCopies(size, null));
		List<Lich_Thi> child2 = new ArrayList<>(Collections.nCopies(size, null));

		int crossoverPoint1 = random.nextInt(size) + 1;
		int crossoverPoint2 = random.nextInt(size) + 1;
		int start = Math.min(crossoverPoint1, crossoverPoint2);
		int end = Math.max(crossoverPoint1, crossoverPoint2);

		Set<String> scheduledSubjectsChild1 = new HashSet<>();
		Set<String> scheduledSubjectsChild2 = new HashSet<>();

		// Copy crossover segment while ensuring uniqueness
		for (int i = start; i <= end; i++) {
			if (i < parent1.size() && !scheduledSubjectsChild1.contains(parent1.get(i).getMa_mon_hoc())) {
				child1.set(i, new Lich_Thi(parent1.get(i)));
				scheduledSubjectsChild1.add(parent1.get(i).getMa_mon_hoc());
			}
			if (i < parent2.size() && !scheduledSubjectsChild2.contains(parent2.get(i).getMa_mon_hoc())) {
				child2.set(i, new Lich_Thi(parent2.get(i)));
				scheduledSubjectsChild2.add(parent2.get(i).getMa_mon_hoc());
			}
		}

		// Fill remaining slots ensuring each subject appears exactly once
		fillRemainingUniquePositions(parent2, child1, scheduledSubjectsChild1, size);
		fillRemainingUniquePositions(parent1, child2, scheduledSubjectsChild2, size);

		// Ensure all courses are present in the children
		ensureAllCoursesPresent(child1, parent1, parent2);
		ensureAllCoursesPresent(child2, parent1, parent2);

		return Arrays.asList(child1, child2);
	}

	private void ensureAllCoursesPresent(List<Lich_Thi> child, List<Lich_Thi> parent1, List<Lich_Thi> parent2) {
		Set<String> coursesInChild = child.stream()
				.filter(Objects::nonNull)
				.map(Lich_Thi::getMa_mon_hoc)
				.collect(Collectors.toSet());

		Set<String> allCourses = Stream.concat(
						parent1.stream().map(Lich_Thi::getMa_mon_hoc),
						parent2.stream().map(Lich_Thi::getMa_mon_hoc))
				.collect(Collectors.toSet());

		// Add missing courses to the child
		for (String course : allCourses) {
			if (!coursesInChild.contains(course)) {
				// Find the exam for the missing course from either parent
				Lich_Thi missingExam = Stream.concat(parent1.stream(), parent2.stream())
						.filter(exam -> exam.getMa_mon_hoc().equals(course))
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

	private void fillRemainingUniquePositions(List<Lich_Thi> parent, List<Lich_Thi> child, Set<String> scheduledSubjects, int maxSize) {
		int index = 0;
		for (Lich_Thi exam : parent) {
			if (!scheduledSubjects.contains(exam.getMa_mon_hoc())) {
				// Find the first available empty spot
				while (index < maxSize && (child.get(index) != null)) {
					index++;
				}
				if (index < maxSize) {
					child.set(index, new Lich_Thi(exam));
					scheduledSubjects.add(exam.getMa_mon_hoc());
				}
			}
		}
	}

	private void fillRemainingPositions(List<Lich_Thi> parent, List<Lich_Thi> child,
	                                    int start, int end) {
		int childPos = 0;
		for (int i = 0; i < parent.size(); i++) {
			if (childPos == start) {
				childPos = end + 1;
			}
			if (childPos < child.size() && child.get(childPos) == null) {
				child.set(childPos++, new Lich_Thi(parent.get(i)));
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
							exam.getMa_mon_hoc(),
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
								exam.getMa_mon_hoc(),
								exam.getNgay_thi(),
								Time.valueOf(newTime),
								exam.getMonHoc(),
								exam.getPhong_thi()
						);
					}
					break;

				case 2: // Swap with another exam
					int swapIndex = random.nextInt(schedule.size());
					if (swapIndex != index) {
						Lich_Thi temp = mutated.get(swapIndex);
						mutated.set(swapIndex, mutated.get(index));
						mutated.set(index, temp);
					}
					break;
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
				System.err.println("Error collecting fitness score: " + e.getMessage());
			}
		}

		return fitnessScores;
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
			System.out.printf("Found solution with fitness: %d%n", bestFitness);
		}

		return bestIndex;
	}
	public List<Map.Entry<String, String>> evaluate(List<Lich_Thi_DTO> schedule) {
		List<Dang_Ky> all_regis = dangKyRepository.findAll();
		List<Map.Entry<String, String>> conflictList = new ArrayList<>();

		// Map each student to their registered exams
		Map<String, List<Lich_Thi_DTO>> studentExamsMap = new HashMap<>();
		for (Dang_Ky registration : all_regis) {
			studentExamsMap.putIfAbsent(registration.ma_sinh_vien, new ArrayList<>());
		}

		// Group exams by student
		for (Lich_Thi_DTO lt : schedule) {
			for (Dang_Ky registration : all_regis) {
				if (registration.maMonHoc.equals(lt.getMa_mon_hoc())) {
					studentExamsMap.get(registration.ma_sinh_vien).add(lt);
				}
			}
		}

		// Check for overlapping exams for each student
		for (Map.Entry<String, List<Lich_Thi_DTO>> entry : studentExamsMap.entrySet()) {
			String studentId = entry.getKey();
			List<Lich_Thi_DTO> studentExams = entry.getValue();

			// Sort exams by date and time
			studentExams.sort(Comparator.comparing(Lich_Thi_DTO::getNgay_thi)
					.thenComparing(Lich_Thi_DTO::getGio_thi));

			// Check if the student has any overlapping exams
			for (int i = 0; i < studentExams.size() - 1; i++) {
				Lich_Thi_DTO currentExam = studentExams.get(i);
				Lich_Thi_DTO nextExam = studentExams.get(i + 1);

				// Calculate the end time of the current exam
				LocalTime nextStartTime = nextExam.getGio_thi().toLocalTime();
				LocalTime currentEndTime = currentExam.getGio_thi().toLocalTime().plusMinutes(currentExam.getThoi_luong_thi());
				// Calculate the start time of the next exam

				// Check if two exams overlap (current exam end time > next exam start time)
				if (currentExam.getNgay_thi().equals(nextExam.getNgay_thi()) && currentEndTime.isAfter(nextStartTime)) {
					// Log the conflicting exams and student
					System.out.printf("Conflict detected for student %s: %s (day %s, from %s - to %s) and %s (day %s, from %s - to %s) \n",
							studentId,
							currentExam.getTen_mon_hoc(),
							currentExam.getNgay_thi().toString(),
							currentExam.getGio_thi().toString(),
							currentEndTime.toString(),
							nextExam.getTen_mon_hoc(),
							nextExam.getNgay_thi().toString(),
							currentExam.getGio_thi().toString(),
							nextStartTime.toString());
					conflictList.add(new AbstractMap.SimpleEntry<>(currentExam.getTen_mon_hoc(), nextExam.getTen_mon_hoc()));
				}
			}
		}

		return conflictList;
	}

	// Helper method to calculate the end time of the exam by adding the duration
	private Timestamp calculateEndTime(Lich_Thi_DTO exam) {
		Timestamp startTime = combineDateAndTime(exam.getNgay_thi(), exam.getGio_thi());
		long durationInMillis = exam.getThoi_luong_thi() * 60 * 1000;  // Convert duration from minutes to milliseconds
		return new Timestamp(startTime.getTime() + durationInMillis);  // Add duration to start time
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

		public int getBestIndex() {
			return bestIndex;
		}

		public void logStatistics(int generation) {
			System.out.printf("Generation %d stats:%n", generation);
			System.out.printf("Best Fitness: %d%n", bestFitness);
			System.out.printf("Worst Fitness: %d%n", worstFitness);
			System.out.printf("Average Fitness: %.2f%n", averageFitness);
			System.out.println("--------------------");
		}
	}

	// Enhanced findBestSolution with statistics
	private int findBestSolution(List<Integer> fitnessScores, int generation) {
		PopulationStatistics stats = new PopulationStatistics(fitnessScores);

		// Log statistics every N generations or when significant improvement is found
		if (generation % 10 == 0 || stats.bestFitness >= 900) {
			stats.logStatistics(generation);
		}

		return stats.getBestIndex();
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

	// Utility method to set log interval
	public void setLogInterval(int interval) {
		this.logInterval = interval;
	}

}