package language;

import utils.Rng;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Language {
	static final int MAX_TRIES = 10;
	String name;

	Map<Integer, FsmNode> nodes = new HashMap<>();
	List<FsmNode> initializers = new ArrayList<>();

	public FsmNode createNode(List<String> parts) {
		FsmNode node = new FsmNode(nodes.size(), parts);
		nodes.put(node.id, node);
		return node;
	}

	public void prepare() {
		initializers = nodes.values().stream()
				.filter(FsmNode::isInitializing).collect(Collectors.toList());
	}

	public List<String> getWords(int number, int minLength, int maxLength) {
		if (initializers.isEmpty()) prepare();
		Set<String> words = new HashSet<>();
		int tries = 0;
		while((words.size() < number) && tries++ < MAX_TRIES) {
			IntStream.range(0, number - words.size() / 2).mapToObj(i ->
					initializers.get(Rng.intInRange(0, initializers.size() - 1)).run(minLength, maxLength)
			).filter(Objects::nonNull
			).forEach(word -> {
				System.out.printf(" = %s%n", word);
				words.add(word);
			});
		}
		int min = Math.min(number, words.size());
		try {
			return new ArrayList<>(words).subList(0, min);
		} catch (Exception e) {
			return new ArrayList<>(words);
		}
	}

	public boolean isValidWord(String word) {
		return initializers.stream().anyMatch(node ->
			node.canMakeWord(word)
		);
	}

	public static Language getLanguageFromStt(List<String> rows) {
		Language l = new Language();
		for (String r : rows) {
			l.rowFromStt(r);
		}
		return l;
	}

	public void rowFromStt(String row) {
		if (row.isEmpty()) return;
		Matcher m = FsmNode.rowPattern.matcher(row);
		if (m.find()) {
			String type = m.group("type");
			if (type.equals("S")) {
				setInitializerNodesFromStt(m.group("links"));
			} else if (type.equals("N")) {
				int id = Integer.parseInt(m.group("id"));
				nodes.putIfAbsent(id, new FsmNode(id, Collections.emptyList()));
				FsmNode node = nodes.get(id);
				node.setNodeBySttRow(this, m.group("parts"), m.group("links"));
			}
		}
	}

	void setInitializerNodesFromStt(String row) {
		List<String> links = Arrays.asList(row.split("\\s*\\|\\s*"));
		links.forEach(link -> {
			Matcher m = FsmNode.linkPattern.matcher(link);
			if (m.find()) {
				if (!m.group("type").equals("N")) return;
				int id = Integer.parseInt(m.group("id"));
				nodes.putIfAbsent(id, new FsmNode(id, Collections.emptyList()));
				FsmNode node = nodes.get(id);
				node.setProperties(true, node.terminating);
				initializers.add(node);
			}
		});
	}


	public List<String> asTransitionTable() {
		List<String> rows = new ArrayList<>();
		rows.add(getInitialSttRow());
		rows.addAll(nodes.values().stream().sorted(Comparator.comparingInt(FsmNode::getId))
				.map(FsmNode::toSttRow).collect(Collectors.toList()));
		return rows;
	}

	private String getInitialSttRow() {
		StringJoiner joiner = new StringJoiner(" ; ");
		joiner.add("S");
		joiner.add("_");
		String linksAsStrings = initializers.stream().sorted(Comparator.comparingInt(FsmNode::getId))
				.map(i -> "N" + i.getId())
				.collect(Collectors.joining(" | "));
		joiner.add(linksAsStrings);
		return joiner.toString();
	}


	public FsmNode getOrCreateNodeById(int linkId) {
		nodes.putIfAbsent(linkId, new FsmNode(linkId, Collections.emptyList()));
		return nodes.get(linkId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Language language = (Language) o;
		boolean nodesMatch = nodes.values().stream().allMatch(node ->
				Optional.ofNullable(language.nodes.get(node.id)).map(otherNode ->
						otherNode.equals(node)).orElse(false)
		);
		boolean initializersMatch = initializers.stream().allMatch(node ->
				language.initializers.stream().anyMatch(otherNode -> node.id == otherNode.id)
		);
		return nodesMatch && initializersMatch;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodes.values().stream().map(Object::hashCode).toArray());
	}
}
