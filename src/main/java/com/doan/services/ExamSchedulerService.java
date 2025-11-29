package com.doan.services;

import com.doan.controller.Common;
import com.doan.dto.Lich_Thi_Option;
import com.doan.model.*;
import org.springframework.stereotype.Service;
import java.util.stream.IntStream;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ExamSchedulerService {
	private final ThreadLocalRandom random = ThreadLocalRandom.current();
	private long totalDays = 2L;
	private LocalDate dateFrom = LocalDate.now();
	private int populationSize = 200;
	private double crossoverRate = 0.85;
	private double mutationRate = 0.15;
	private int startHour = 9;
	private int endHour = 18;
	private int totalCourse = 100;
	private int maxExamPerDay = 5;

	private List<LocalTime> timeSlots;
	private Map<String, List<String>> studentCourseMap = new HashMap<>();
	private List<Subject> selected_course = new ArrayList<>();
	private List<Registration> registrations = new ArrayList<>();

	private List<LocalTime> generateTimeSlots(int startHour) {
		return List.of(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(15, 0));
	}

	public List<Schedule> generateExamSchedule(Lich_Thi_Option options) {
		System.out.println("[INFO] Starting exam scheduling without MySQL");

		List<String> selectedSubjects = options.getSelectedSubjects();
		List<Subject> courses = Cache.cache.subjects.values().stream()
				.filter(sub -> selectedSubjects.contains(sub.name))
				.distinct()
				.collect(Collectors.toList());
		List<Registration> regis = new ArrayList<>();
		for (Student s : Cache.cache.students.values()) {
			for (Registration r : s.registrations) {
				if (selectedSubjects.contains(r.getTen_mon_hoc())) {
					regis.add(r);
				}
			}
		}

		this.selected_course = courses;
		this.registrations = regis;
		this.studentCourseMap = initializeStudentCourseMap(regis);

		this.totalDays = ChronoUnit.DAYS.between(LocalDate.parse(options.getDayFrom()), LocalDate.parse(options.getDayTo())) + 1;
		this.populationSize = options.getPopulationSize();
		this.crossoverRate = options.getCrossoverRate();
		this.mutationRate = options.getMutationRate();
		this.startHour = options.getHourFromInt();
		this.endHour = options.getHourToInt();
		this.dateFrom = LocalDate.parse(options.getDayFrom());
		this.totalCourse = courses.size();
		this.timeSlots = generateTimeSlots(this.startHour);
		this.maxExamPerDay = options.getMaxExamPerDay();

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<List<Schedule>> population = generateInitialPopulation(courses);
		List<Schedule> bestSchedule = null;
		int bestFitness = Integer.MIN_VALUE;

		try {
			for (int generation = 0; generation < options.getMaxGenerations(); generation++) {
				List<Future<Integer>> fitnessTasks = evaluatePopulation(population, executor);
				List<Integer> fitnessScores = collectFitnessScores(fitnessTasks);

				int bestIdx = findBestSolution(fitnessScores);
				if (bestSchedule == null || fitnessScores.get(bestIdx) > bestFitness) {
					bestSchedule = new ArrayList<>(population.get(bestIdx));
					bestFitness = fitnessScores.get(bestIdx);
				}

				System.out.println("Generation: " + generation + " fitness " + bestFitness);
				if (bestFitness >= 970) break;

				population = evolvePopulation(population, fitnessScores);
			}
		} finally {
			executor.shutdown();
		}

		return bestSchedule;
	}

	private Map<String, List<String>> initializeStudentCourseMap(List<Registration> regis) {
		Map<String, List<String>> map = new HashMap<>();
		for (Registration r : regis) {
			map.computeIfAbsent(r.getMa_sinh_vien(), k -> new ArrayList<>()).add(r.getTen_mon_hoc());
		}
		return map;
	}

	private List<List<Schedule>> generateInitialPopulation(List<Subject> courses) {
		List<List<Schedule>> population = new ArrayList<>();
		for (int i = 0; i < populationSize; i++) {
			List<Schedule> schedule = generateConflictAwareSchedule(courses);
			// Ensure all subjects are scheduled
			schedule = ensureAllSubjectsScheduled(schedule, courses);
			population.add(schedule);
		}
		return population;
	}

	private List<Schedule> generateConflictAwareSchedule(List<Subject> courses) {
		List<Schedule> schedule = new ArrayList<>();
		List<Subject> unscheduled = new ArrayList<>(courses);

		// Sort subjects by number of student registrations (descending) to prioritize harder-to-schedule subjects
		unscheduled.sort((a, b) -> getStudentCount(b.name) - getStudentCount(a.name));

		int maxRetries = 10; // Increased retries

		for (Subject subj : unscheduled) {
			boolean placed = false;
			int retry = 0;

			while (!placed && retry < maxRetries) {
				retry++;

				// Get all possible date-time combinations
				List<Schedule> candidates = generateCandidateSchedules(subj);
				Collections.shuffle(candidates);

				for (Schedule candidate : candidates) {
					if (isValidExamPlacement(candidate, schedule)) {
						schedule.removeIf(s -> s.getSubjectName().equals(candidate.getSubjectName()));
						schedule.add(candidate);
						placed = true;
						break;
					}
				}
			}

			if (!placed) {
				// Force placement with relaxed constraints if still not placed
				Schedule forcedSchedule = forceScheduleSubject(subj, schedule);
				if (forcedSchedule != null) {
					schedule.removeIf(s -> s.getSubjectName().equals(forcedSchedule.getSubjectName()));
					schedule.add(forcedSchedule);
					System.out.printf("[WARN] Force scheduled subject: %s\n", subj.name);
				} else {
					System.err.printf("[ERROR] Failed to schedule subject: %s after %d retries\n", subj.name, maxRetries);
				}
			}
		}
		return schedule;
	}

	private List<Schedule> generateCandidateSchedules(Subject subject) {
		List<Schedule> candidates = new ArrayList<>();

		for (long dayOffset = 0; dayOffset < totalDays; dayOffset++) {
			LocalDate date = dateFrom.plusDays(dayOffset);
			for (LocalTime time : timeSlots) {
				LocalTime endTime = time.plusMinutes(subject.duration);

				// Check basic time constraints
				if (time.isBefore(LocalTime.of(startHour, 0)) ||
						endTime.isAfter(LocalTime.of(endHour, 0))) {
					continue;
				}

				Schedule candidate = new Schedule(subject.name, Date.valueOf(date),
						Time.valueOf(time), subject, "Room-1");
				candidates.add(candidate);
			}
		}

		return candidates;
	}

	private Schedule forceScheduleSubject(Subject subject, List<Schedule> existingSchedule) {
		// Try to find any valid slot, even if it violates some soft constraints
		for (long dayOffset = 0; dayOffset < totalDays; dayOffset++) {
			LocalDate date = dateFrom.plusDays(dayOffset);
			for (LocalTime time : timeSlots) {
				LocalTime endTime = time.plusMinutes(subject.duration);

				// Only check hard constraints (working hours)
				if (time.isBefore(LocalTime.of(startHour, 0)) ||
						endTime.isAfter(LocalTime.of(endHour, 0))) {
					continue;
				}

				Schedule candidate = new Schedule(subject.name, Date.valueOf(date),
						Time.valueOf(time), subject, "Room-1");

				// Check only for direct time conflicts (not student conflicts)
				if (hasDirectTimeConflict(candidate, existingSchedule)) {
					continue;
				}

				return candidate;
			}
		}

		// Last resort: place it anyway at the first available time slot
		if (!timeSlots.isEmpty()) {
			LocalTime time = timeSlots.get(0);
			LocalDate date = dateFrom;
			return new Schedule(subject.name, Date.valueOf(date),
					Time.valueOf(time), subject, "Room-1");
		}

		return null;
	}

	private boolean hasDirectTimeConflict(Schedule newExam, List<Schedule> schedule) {
		LocalTime start = newExam.getTime().toLocalTime();
		LocalTime end = start.plusMinutes(newExam.getSubject().duration);

		for (Schedule s : schedule) {
			if (!s.getDate().equals(newExam.getDate())) continue;
			LocalTime sStart = s.getTime().toLocalTime();
			LocalTime sEnd = sStart.plusMinutes(s.getSubject().duration);

			// Check for time overlap
			if (!(end.isBefore(sStart) || start.isAfter(sEnd))) {
				return true;
			}
		}
		return false;
	}

	private List<Schedule> ensureAllSubjectsScheduled(List<Schedule> schedule, List<Subject> courses) {
		Set<String> scheduledSubjects = schedule.stream()
				.map(Schedule::getSubjectName)
				.collect(Collectors.toSet());

		List<Subject> missingSubjects = courses.stream()
				.filter(course -> !scheduledSubjects.contains(course.name))
				.collect(Collectors.toList());

		if (!missingSubjects.isEmpty()) {
			System.out.printf("[INFO] Found %d missing subjects, attempting to schedule them\n", missingSubjects.size());

			for (Subject missing : missingSubjects) {
				Schedule forcedSchedule = forceScheduleSubject(missing, schedule);
				if (forcedSchedule != null) {
					schedule.add(forcedSchedule);
					System.out.printf("[INFO] Successfully scheduled missing subject: %s\n", missing.name);
				} else {
					System.err.printf("[ERROR] Could not schedule missing subject: %s\n", missing.name);
				}
			}
		}

		return schedule;
	}

	private int getStudentCount(String subjectName) {
		return (int) studentCourseMap.values().stream()
				.mapToLong(courses -> courses.contains(subjectName) ? 1 : 0)
				.sum();
	}

	private boolean isValidExamPlacement(Schedule newExam, List<Schedule> schedule) {
		LocalTime start = newExam.getTime().toLocalTime();
		LocalTime end = start.plusMinutes(newExam.getSubject().duration);

		for (Schedule s : schedule) {
			if (!s.getDate().equals(newExam.getDate())) continue;
			LocalTime sStart = s.getTime().toLocalTime();
			LocalTime sEnd = sStart.plusMinutes(s.getSubject().duration);
			if (!(end.isBefore(sStart) || start.isAfter(sEnd))) {
				for (Map.Entry<String, List<String>> entry : studentCourseMap.entrySet()) {
					if (entry.getValue().contains(newExam.getSubjectName()) &&
							entry.getValue().contains(s.getSubjectName())) return false;
				}
			}
		}
		return true;
	}

	private List<Future<Integer>> evaluatePopulation(List<List<Schedule>> population, ExecutorService executor) {
		return population.stream()
				.map(sch -> executor.submit(() -> calculateFitness(sch)))
				.collect(Collectors.toList());
	}

	private List<Integer> collectFitnessScores(List<Future<Integer>> futures) {
		List<Integer> scores = new ArrayList<>();
		for (Future<Integer> f : futures) {
			try {
				scores.add(f.get(5, TimeUnit.SECONDS));
			} catch (Exception e) {
				scores.add(0);
			}
		}
		return scores;
	}

	private int findBestSolution(List<Integer> scores) {
		int best = 0;
		for (int i = 1; i < scores.size(); i++) {
			if (scores.get(i) > scores.get(best)) best = i;
		}
		return best;
	}

	private int calculateFitness(List<Schedule> schedule) {
		int fitness = 1000;

		// Penalty for missing subjects (high penalty)
		Set<String> scheduledSubjects = schedule.stream()
				.map(Schedule::getSubjectName)
				.collect(Collectors.toSet());
		Set<String> requiredSubjects = selected_course.stream()
				.map(s -> s.name)
				.collect(Collectors.toSet());
		int missingCount = requiredSubjects.size() - scheduledSubjects.size();
		fitness -= missingCount * 200; // High penalty for missing subjects

		// Penalty for time constraint violations
		for (Schedule exam : schedule) {
			if (exam.getTime().toLocalTime().isAfter(LocalTime.of(endHour, 0))) fitness -= 100;
		}

		// Penalty for student conflicts
		int conflicts = countStudentConflicts(schedule);
		fitness -= conflicts * 50;

		return Math.max(0, fitness);
	}

	private int countStudentConflicts(List<Schedule> schedule) {
		int conflicts = 0;
		for (Student student : Cache.cache.students.values()) {
			List<Schedule> studentExams = schedule.stream()
					.filter(s -> student.registrations.stream().anyMatch(r -> r.getTen_mon_hoc().equals(s.getSubjectName())))
					.collect(Collectors.toList());

			for (int i = 0; i < studentExams.size(); i++) {
				for (int j = i + 1; j < studentExams.size(); j++) {
					Schedule a = studentExams.get(i);
					Schedule b = studentExams.get(j);
					if (!a.getDate().equals(b.getDate())) continue;
					LocalTime startA = a.getTime().toLocalTime();
					LocalTime endA = startA.plusMinutes(a.getSubject().duration - 1);
					LocalTime startB = b.getTime().toLocalTime();
					LocalTime endB = startB.plusMinutes(b.getSubject().duration - 1);

					// Two intervals overlap if: !(endA <= startB || startA >= endB)
					// Or equivalently: endA > startB && startA < endB
					if (endA.isAfter(startB) && startA.isBefore(endB)) {
						conflicts++;
					}				}
			}
		}
		return conflicts;
	}

	private List<List<Schedule>> evolvePopulation(List<List<Schedule>> population, List<Integer> scores) {
		List<List<Schedule>> newPop = new ArrayList<>();
		int eliteCount = populationSize / 10;
		List<Integer> sorted = IntStream.range(0, scores.size())
				.boxed()
				.sorted((i, j) -> scores.get(j) - scores.get(i))
				.collect(Collectors.toList());

		for (int i = 0; i < eliteCount; i++) newPop.add(population.get(sorted.get(i)));

		while (newPop.size() < populationSize) {
			List<Schedule> p1 = population.get(random.nextInt(populationSize));
			List<Schedule> p2 = population.get(random.nextInt(populationSize));
			List<List<Schedule>> children = crossover(p1, p2);

			List<Schedule> child1 = mutate(children.get(0));
			child1 = ensureAllSubjectsScheduled(child1, selected_course);
			newPop.add(child1);

			if (newPop.size() < populationSize) {
				List<Schedule> child2 = mutate(children.get(1));
				child2 = ensureAllSubjectsScheduled(child2, selected_course);
				newPop.add(child2);
			}
		}
		return newPop;
	}

	private List<List<Schedule>> crossover(List<Schedule> p1, List<Schedule> p2) {
		List<Schedule> c1 = new ArrayList<>();
		List<Schedule> c2 = new ArrayList<>();
		for (int i = 0; i < p1.size(); i++) {
			c1.add(new Schedule(p1.get(i)));
			c2.add(new Schedule(p2.get(i)));
		}
		return List.of(c1, c2);
	}

	private List<Schedule> mutate(List<Schedule> schedule) {
		if (random.nextDouble() > mutationRate) return schedule;
		List<Schedule> mutated = new ArrayList<>(schedule);
		int index = random.nextInt(schedule.size());
		Schedule exam = mutated.get(index);

		LocalDate newDate = exam.getDate().toLocalDate();
		LocalTime newTime = exam.getTime().toLocalTime();

		int choice = random.nextInt(3); // 0 = time, 1 = date, 2 = both
		if (choice == 0 || choice == 2) {
			newTime = getRandomValidTimeSlot(exam.getSubject().duration);
		}
		if (choice == 1 || choice == 2) {
			long days = totalDays;
			newDate = dateFrom.plusDays(random.nextLong(days));
		}

		Schedule mutatedExam = new Schedule(exam.getSubjectName(), Date.valueOf(newDate), Time.valueOf(newTime), exam.getSubject(), exam.getRoom());
		mutated.set(index, mutatedExam);
		return mutated;
	}

	private LocalTime getRandomValidTimeSlot(int duration) {
		List<LocalTime> valid = new ArrayList<>(timeSlots);
		return valid.isEmpty() ? null : valid.get(random.nextInt(valid.size()));
	}

	public String evaluate(List<Schedule> schedule) {
		StringBuilder result = new StringBuilder();

		// Rule 2: all selected subjects present
		Set<String> scheduledSubjects = schedule.stream().map(Schedule::getSubjectName).collect(Collectors.toSet());
		Set<String> selectedSubjects = selected_course.stream().map(s -> s.name).collect(Collectors.toSet());
		Set<String> missingSubjects = new HashSet<>(selectedSubjects);
		missingSubjects.removeAll(scheduledSubjects);
		if (!missingSubjects.isEmpty()) {
			result.append("Missing subjects: ").append(missingSubjects).append("\n");
		}

		// Rule 3: no duplicate subjects
		Set<String> duplicates = schedule.stream()
				.map(Schedule::getSubjectName)
				.filter(name -> Collections.frequency(scheduledSubjects, name) > 1)
				.collect(Collectors.toSet());
		if (!duplicates.isEmpty()) {
			result.append("Duplicate subjects: ").append(duplicates).append("\n");
		}

		// Rule 1: no student conflict
		for (Student student : Cache.cache.students.values()) {
			List<Schedule> studentExams = schedule.stream()
					.filter(s -> student.registrations.stream().anyMatch(r -> r.getTen_mon_hoc().equals(s.getSubjectName())))
					.collect(Collectors.toList());

			for (int i = 0; i < studentExams.size(); i++) {
				for (int j = i + 1; j < studentExams.size(); j++) {
					Schedule a = studentExams.get(i);
					Schedule b = studentExams.get(j);
					if (!a.getDate().equals(b.getDate())) continue;
					LocalTime startA = a.getTime().toLocalTime();
					LocalTime endA = startA.plusMinutes(a.getSubject().duration - 1);
					LocalTime startB = b.getTime().toLocalTime();
					LocalTime endB = startB.plusMinutes(b.getSubject().duration - 1);
					if (!(endA.isBefore(startB) || startA.isAfter(endB))) {
						result.append(String.format("Student %s has conflict between %s and %s\n",
								student.name, a.getSubjectName(), b.getSubjectName()));
					}
				}
			}
		}

		return result.toString().isEmpty() ? "Schedule is valid." : result.toString();
	}
}