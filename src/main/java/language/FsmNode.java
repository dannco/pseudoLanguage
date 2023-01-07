package language;

import utils.Rng;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FsmNode {
	static String sttRowPattern = "^(?<type>[S|N])(?<id>\\d+)?\\s*;\\s*(?<parts>.*)\\s*;\\s*(?<links>.*)$";
	public static Pattern rowPattern = Pattern.compile(sttRowPattern);

	static String sttLinkPattern = "(?<type>[ON])(?<id>\\d+)?(\\((?<weight>\\d+)\\))?";
	public static Pattern linkPattern = Pattern.compile(sttLinkPattern);

	static String sttPartPattern = "^(?<value>\\w+)(\\((?<weight>\\d+)\\))?$";
	static Pattern partPattern = Pattern.compile(sttPartPattern);

	int linkWeight = 0;
	int partWeight = 0;
	int run = 0;
	public final int id;

	boolean terminating;
	boolean initializing;
	NavigableMap<Integer, String> parts = new TreeMap<>();
	NavigableMap<Integer, FsmNode> links = new TreeMap<>();


	FsmNode(int id, List<String> parts) {
		this.id = id;
		parts.forEach(this::addPart);
	}

	public String run(int minLength, int maxLengthThreshold) {
		run++;
		FsmNode current = this;
		StringBuilder builder = new StringBuilder();
		Optional.ofNullable(current.get()).ifPresent(builder::append);
		System.out.printf("%d: (%d)%s", run, id, builder);
		boolean terminate = terminates();
		while (!terminate) {
			current = current.getNextNode();
			if (Objects.isNull(current))
				break;
			System.out.printf(" + (%d)", current.id);
			String nextStr = current.get();
			if (Objects.isNull(nextStr))
				System.out.print("NULL");
			else {
				System.out.printf(":%s", nextStr);
				builder.append(nextStr);
			}
			int wordLength = builder.length();
			terminate = current.terminating && wordLength >= minLength && // basic conditions to terminate
					(Rng.intInRange(0, Math.max(0, 1 + maxLengthThreshold - wordLength)) == 0
							|| Rng.intInRange(0, links.size() + 1) == 0)
					|| current.terminates();
		}
		return builder.toString();
	}

	private FsmNode getNextNode() {
		return links.higherEntry(Rng.intInRange(0, linkWeight - 1)).getValue();
	}

	private boolean terminates() {
		return links.isEmpty() || terminating && Rng.intInRange(0, links.size() + 1) == 0;
	}

	String get() {
		if (!parts.isEmpty()) {
			return Optional.ofNullable(parts.higherEntry(Rng.intInRange(0, partWeight - 1))
			).map(Map.Entry::getValue).orElse(null);
		}
		return null;
	}

	public boolean isInitializing() {
		return initializing;
	}

	public boolean canMakeWord(String word) {
		List<String> newWords = parts.values().stream().filter(word::startsWith).map(part ->
			word.substring(part.length())
		).distinct().collect(Collectors.toList());
		if (newWords.stream().anyMatch(String::isEmpty) && terminating) return true;
		return newWords.stream().anyMatch(newWord ->
				links.values().stream().anyMatch(node -> node.canMakeWord(newWord))
		);
	}

	public void setProperties(boolean initializing, boolean terminating) {
		this.initializing = initializing;
		this.terminating = terminating; // must be terminating node if no links are provided
	}

	void setLinkByStt(Language lang, String input) {
		Matcher m = linkPattern.matcher(input);
		if (!m.find()) return;
		if (m.group("type").equals("O")) {
			terminating = true;
			return;
		}
		int linkId = Integer.parseInt(m.group("id"));
		int weight = Integer.parseInt(Optional.ofNullable(m.group("weight")).orElse("1"));
		FsmNode node = lang.getOrCreateNodeById(linkId);
		addLink(node, weight);
	}

	public boolean addLink(FsmNode node) {
		return addLink(node, 1);
	}

	public boolean addLink(FsmNode node, int weight) {
		if (weight <= 0) return false;
		linkWeight += weight;
		links.put(linkWeight, node);
		return true;
	}

	void addPartFromStt(String input) {
		Matcher m = partPattern.matcher(input.trim());
		if (!m.find()) return;

		String part = m.group("value");
		int weight = Integer.parseInt(Optional.ofNullable(m.group("weight")).orElse("1"));
		addPart(part, weight);
	}

	public void addPart(String part) {
		addPart(part, 1);
	}
	public void addPart(String part, int weight) {
		if (weight <= 0) return;
		partWeight += weight;
		parts.put(partWeight, part);
	}

	public int getId() {
		return id;
	}

	public String toSttRow() {
		StringJoiner joiner = new StringJoiner(" ; ");
		joiner.add("N" + id);
		int[] weight = new int[]{0, 0};
		int PARTS = 0;
		int LINKS = 1;

		joiner.add(parts.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey))
				.map(entry -> {
					String value = String.format("%s(%d)", entry.getValue(), entry.getKey() - weight[PARTS]);
					weight[PARTS] = entry.getKey();
					return value;
				}).collect(Collectors.joining(" | ")));
		String linksAsStrings = links.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey))
				.map(i -> {
					String value = String.format("N%d(%d)", i.getValue().getId(), i.getKey() - weight[LINKS]);
					weight[LINKS] = i.getKey();
					return value;
				})
				.collect(Collectors.joining(" | "));
		if (terminating) linksAsStrings += " | O";
		joiner.add(linksAsStrings);
		return joiner.toString();
	}

	public void setNodeBySttRow(Language lang, String parts, String links) {
		Arrays.stream(parts.split(" \\| ")).forEach(this::addPartFromStt);
		Arrays.stream(links.split(" \\| ")).forEach(link -> setLinkByStt(lang, link));
		terminating = terminating || this.links.size() == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FsmNode fsaNode = (FsmNode) o;
		return linkWeight == fsaNode.linkWeight &&
				partWeight == fsaNode.partWeight &&
				id == fsaNode.id &&
				terminating == fsaNode.terminating &&
				initializing == fsaNode.initializing &&
				parts.entrySet().stream().allMatch(entry ->
						Optional.ofNullable(fsaNode.parts.get(entry.getKey())).map(str ->
								str.equals(entry.getValue())).orElse(false)
				) &&
				links.entrySet().stream().allMatch(entry ->
					Optional.ofNullable(fsaNode.links.get(entry.getKey())).map(otherNode ->
							otherNode.id == entry.getValue().id).orElse(false)
				);
	}

	@Override
	public int hashCode() {
		return Objects.hash(linkWeight, partWeight, id, terminating, initializing);
	}
}
