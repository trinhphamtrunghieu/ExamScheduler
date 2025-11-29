package com.doan.services;

import com.doan.dto.*;
import com.doan.model.*;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ExamSchedulerService2 {
	public List<Registration> cur_registration = new ArrayList<>();
	/**
	 * Generates exam schedule with minimum number of groups (time slots)
	 */
	public List<Schedule> generateExamSchedule(Lich_Thi_Option options) {
		// 1. Load subjects and registrations
		Cache cache = Cache.cache;
		List<Subject> subjects = cache.subjects.entrySet()
				.stream()
				.filter(entry -> options.getSelectedSubjects().contains(entry.getValue().name))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
		List<Registration> registrations = new ArrayList<>();
		for (Student student : cache.students.values()) {
			registrations.addAll(student.registrations);
		}
		cur_registration = registrations;
		// 2. Build index maps
		Map<String, Integer> subjectToIndex = buildSubjectIndexMap(subjects);
		Map<Integer, String> indexToSubject = buildIndexToSubjectMap(subjectToIndex);
		Map<String, Subject> nameToSubject = buildNameToSubjectMap(subjects);

		// 3. Build conflict graph
		List<Set<Integer>> conflictGraph = buildConflictGraph(registrations, subjectToIndex);

		// 4. Find minimum coloring (grouping) using improved algorithm
		Map<Integer, Integer> subjectToGroup = findMinimumColoring(conflictGraph);

		// 5. Generate available time slots
		List<LocalDateTime> availableSlots = generateAvailableTimeSlots(options);
		// 6. Create exam schedule
		return createExamSchedule(subjectToGroup, indexToSubject, nameToSubject, availableSlots);
	}

	/**
	 * Builds subject name to index mapping
	 */
	private Map<String, Integer> buildSubjectIndexMap(List<Subject> subjects) {
		Map<String, Integer> subjectToIndex = new HashMap<>();
		int count = 0;
		for (Subject subject : subjects) {
			String name = subject.getName();
			if (!subjectToIndex.containsKey(name)) {
				subjectToIndex.put(name, count++);
			}
		}
		return subjectToIndex;
	}

	/**
	 * Builds index to subject name mapping
	 */
	private Map<Integer, String> buildIndexToSubjectMap(Map<String, Integer> subjectToIndex) {
		return subjectToIndex.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	/**
	 * Builds subject name to Mon_Hoc object mapping
	 */
	private Map<String, Subject> buildNameToSubjectMap(List<Subject> subjects) {
		return subjects.stream()
				.collect(Collectors.toMap(Subject::getName, subject -> subject, (a, b) -> a));
	}

	/**
	 * Builds conflict graph representing which subjects cannot be scheduled together
	 */
	private List<Set<Integer>> buildConflictGraph(List<Registration> registrations,
	                                              Map<String, Integer> subjectToIndex) {
		int subjectCount = subjectToIndex.size();
		List<Set<Integer>> conflictGraph = new ArrayList<>(subjectCount);
		for (int i = 0; i < subjectCount; i++) {
			conflictGraph.add(new HashSet<>());
		}

		// Group registrations by student
		Map<String, List<String>> studentCourses = registrations.stream()
				.collect(Collectors.groupingBy(
						Registration::getMa_sinh_vien,
						Collectors.mapping(Registration::getTen_mon_hoc, Collectors.toList())
				));

		// Add conflicts between subjects taken by the same student
		for (List<String> courses : studentCourses.values()) {
			List<Integer> courseIndices = courses.stream()
					.map(subjectToIndex::get)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			// Add bidirectional conflicts
			for (int i = 0; i < courseIndices.size(); i++) {
				for (int j = i + 1; j < courseIndices.size(); j++) {
					int subjectA = courseIndices.get(i);
					int subjectB = courseIndices.get(j);
					conflictGraph.get(subjectA).add(subjectB);
					conflictGraph.get(subjectB).add(subjectA);
				}
			}
		}

		return conflictGraph;
	}

	/**
	 * Finds minimum coloring using Welsh-Powell algorithm with improvements
	 */
	private Map<Integer, Integer> findMinimumColoring(List<Set<Integer>> conflictGraph) {
		int n = conflictGraph.size();
		Map<Integer, Integer> coloring = new HashMap<>();

		// 1. Sort vertices by degree (descending) - Welsh-Powell heuristic
		List<Integer> vertices = IntStream.range(0, n)
				.boxed()
				.sorted((a, b) -> Integer.compare(
						conflictGraph.get(b).size(),
						conflictGraph.get(a).size()
				))
				.collect(Collectors.toList());

		// 2. Greedily assign colors
		int maxColor = -1;
		for (int vertex : vertices) {
			Set<Integer> usedColors = getUsedColors(vertex, conflictGraph, coloring);
			int color = findSmallestAvailableColor(usedColors);
			coloring.put(vertex, color);
			maxColor = Math.max(maxColor, color);
		}

		System.out.println("Minimum number of exam groups needed: " + (maxColor + 1));
		return coloring;
	}

	/**
	 * Gets colors used by neighboring vertices
	 */
	private Set<Integer> getUsedColors(int vertex, List<Set<Integer>> conflictGraph,
	                                   Map<Integer, Integer> coloring) {
		return conflictGraph.get(vertex).stream()
				.filter(coloring::containsKey)
				.map(coloring::get)
				.collect(Collectors.toSet());
	}

	/**
	 * Finds the smallest available color (group number)
	 */
	private int findSmallestAvailableColor(Set<Integer> usedColors) {
		int color = 0;
		while (usedColors.contains(color)) {
			color++;
		}
		return color;
	}

	/**
	 * Generates all available time slots from the given date range
	 */
	private List<LocalDateTime> generateAvailableTimeSlots(Lich_Thi_Option options) {
		List<LocalDateTime> slots = new ArrayList<>();

		// Generate slots for multiple days if needed
		LocalDate startDate = LocalDate.parse(options.getDayFrom());
		LocalDate endDate = LocalDate.parse(options.getDayTo());

		List<LocalTime> dailyTimes = getDefaultTimes();

		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {
			for (LocalTime time : dailyTimes) {
				slots.add(LocalDateTime.of(currentDate, time));
			}
			currentDate = currentDate.plusDays(1);
		}

		return slots;
	}

	/**
	 * Creates the final exam schedule from the coloring result
	 */
	private List<Schedule> createExamSchedule(Map<Integer, Integer> subjectToGroup,
	                                          Map<Integer, String> indexToSubject,
	                                          Map<String, Subject> nameToSubject,
	                                          List<LocalDateTime> availableSlots) {
		List<Schedule> result = new ArrayList<>();

		// Group subjects by their assigned group (color)
		Map<Integer, List<Integer>> groupToSubjects = subjectToGroup.entrySet().stream()
				.collect(Collectors.groupingBy(
						Map.Entry::getValue,
						Collectors.mapping(Map.Entry::getKey, Collectors.toList())
				));

		// Assign time slots to groups
		List<Integer> sortedGroups = groupToSubjects.keySet().stream()
				.sorted()
				.collect(Collectors.toList());

		if (sortedGroups.size() > availableSlots.size()) {
			throw new IllegalStateException(
					String.format("Need %d time slots but only %d available. Consider extending the date range.",
							sortedGroups.size(), availableSlots.size())
			);
		}

		for (int i = 0; i < sortedGroups.size(); i++) {
			int group = sortedGroups.get(i);
			LocalDateTime slotTime = availableSlots.get(i);

			for (int subjectIndex : groupToSubjects.get(group)) {
				String subjectName = indexToSubject.get(subjectIndex);
				Subject subject = nameToSubject.get(subjectName);

				Schedule exam = new Schedule(subjectName,
						java.sql.Date.valueOf(slotTime.toLocalDate()),
						Time.valueOf(slotTime.toLocalTime()),
						subject,
						"to be implement");

				result.add(exam);
			}
		}

		return result;
	}

	/**
	 * Default time slots for each day
	 */
	private List<LocalTime> getDefaultTimes() {
		return List.of(
				LocalTime.of(8, 0),
				LocalTime.of(10, 0),
				LocalTime.of(13, 0),
				LocalTime.of(15, 0)
		);
	}

	/**
	 * Validates the scheduling result - ensures no conflicts exist
	 */
	public boolean validateSchedule(String conflict_res, List<Schedule> schedule, List<Registration> registrations) {
		// Group exams by date and time
		String result = "";
		Map<String, List<Schedule>> timeSlotGroups = schedule.stream()
				.collect(Collectors.groupingBy(exam ->
						exam.getDate().toString() + "_" + exam.getTime().toString()
				));

		// Check for conflicts in each time slot
		for (List<Schedule> examsInSlot : timeSlotGroups.values()) {
			if (hasConflict(result, examsInSlot, registrations)) {
				conflict_res = result;
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if a list of exams has any student conflicts
	 */
	private boolean hasConflict(String res, List<Schedule> exams, List<Registration> registrations) {
		StringBuilder sb = new StringBuilder();
		Set<String> subjectsInSlot = exams.stream()
				.map(Schedule::getSubjectName)
				.collect(Collectors.toSet());

		Map<String, List<String>> studentSubjects = registrations.stream()
				.collect(Collectors.groupingBy(
						Registration::getMa_sinh_vien,
						Collectors.mapping(Registration::getTen_mon_hoc, Collectors.toList())
				));

		// Check if any student is registered for multiple subjects in this slot
		for (List<String> studentCourses : studentSubjects.values()) {
			long conflictCount = studentCourses.stream()
					.filter(subjectsInSlot::contains)
					.count();
			if (conflictCount > 1) {
				sb.append(studentCourses);
			}
		}
		res = sb.toString();
		return false;
	}
}