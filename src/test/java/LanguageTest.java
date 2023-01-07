import org.junit.Test;
import language.FsmNode;
import language.Language;
import utils.FileUtils;
import utils.Rng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class LanguageTest {
	private final static String testLanguage = "gibberish.csv";

	@Test
	public void patriotTest() {
		Language lang = new Language();
		FsmNode node = lang.createNode(Arrays.asList(
				"la", "li", "lu", "le", "lo"
		));
		node.setProperties(true, true);
		node.addLink(node);
		lang.prepare();

		List<String> words = lang.getWords(100, 1, 15);

		assertTrue(lang.isValidWord("lalilulelo"));
		assertFalse(lang.isValidWord("patriots"));
	}

	@Test
	public void testModifiedLanguage() {
		List<String> rows = FileUtils.getFileContents("src/main/resources/languages/gib3.csv");
		Language language = Language.getLanguageFromStt(rows);
		int desiredWords = 1000;
		language.prepare();
		List<String> words = language.getWords(desiredWords, 2, 9);
		assertEquals(words.size(), desiredWords);
	}
	@Test
	public void testFutharkLanguage() {
		List<String> rows = FileUtils.getFileContents("src/main/resources/languages/futhark.csv");
		Language language = Language.getLanguageFromStt(rows);
		int desiredWords = 100;
		language.prepare();
		assertFalse(language.isValidWord("s"));
		List<String> words = language.getWords(desiredWords, 2, 9);

		assertEquals(words.size(), desiredWords);
	}

	public Language gibberishLanguage() {
		Language lang = new Language();
		List<String> vowels = new ArrayList<>(Arrays.asList("a", "i", "u", "o"));
		List<String> consonants = new ArrayList<>(Arrays.asList(
				"b", "d", "g", "k", "m", "r", "s", "t", "p"));

		FsmNode vowelNode = lang.createNode(vowels);
		vowelNode.addPart("e", 3);
		vowels.add("e");

		consonants.addAll(Arrays.asList("l", "n"));
		FsmNode doubleConsonantNode = lang.createNode(
				consonants.stream().map(i -> i + i).collect(Collectors.toList())
		);

		consonants.addAll(Arrays.asList("y", "v"));
		FsmNode consonantCombinationNode = lang.createNode((
				consonants.stream().flatMap(i ->
						consonants.stream().filter(j -> !i.equals(j)).map(j -> i + j)
				).collect(Collectors.toList())
		));
		FsmNode singleConsonantNode = lang.createNode(consonants);

		FsmNode syllabicNode = lang.createNode(consonants.stream().flatMap(i ->
				vowels.stream().map(j -> i+j)
		).collect(Collectors.toList()));
		Stream.of("l", "n").flatMap(i ->
				vowels.stream().map(j -> i+j)
		).forEach(part -> syllabicNode.addPart(part, 2));
		vowels.stream().map(j -> "y"+j).forEach(syllabicNode::addPart);

		FsmNode doubleVowelNode = lang.createNode(
				vowels.stream().flatMap(i ->
						vowels.stream().map(j -> i + j)
				).collect(Collectors.toList())
		);

		FsmNode chaNode = lang.createNode(Arrays.asList("ch", "sh", "sch"));

		vowelNode.setProperties(true, false);
		doubleVowelNode.setProperties(true, false);
		chaNode.setProperties(true, true);
		doubleConsonantNode.setProperties(false, false);
		consonantCombinationNode.setProperties(false, false);
		singleConsonantNode.setProperties(true, true);
		syllabicNode.setProperties(true, true);

		vowelNode.addLink(syllabicNode, 5);
		vowelNode.addLink(doubleConsonantNode, 5);
		vowelNode.addLink(chaNode);
		vowelNode.addLink(singleConsonantNode, 4);
		vowelNode.addLink(consonantCombinationNode, 3);

		doubleVowelNode.addLink(singleConsonantNode);

		chaNode.addLink(vowelNode, 2);
		chaNode.addLink(doubleVowelNode);

		singleConsonantNode.addLink(vowelNode, 2);
		singleConsonantNode.addLink(doubleVowelNode);

		consonantCombinationNode.addLink(vowelNode);
		consonantCombinationNode.addLink(doubleVowelNode);
		doubleConsonantNode.addLink(vowelNode);

		syllabicNode.addLink(doubleConsonantNode, 2);
		syllabicNode.addLink(singleConsonantNode, 2);
		syllabicNode.addLink(syllabicNode, 5);
		syllabicNode.addLink(chaNode);
		lang.prepare();
		return lang;
	}

	@Test
	public void gibberishTest() {
		Language lang = gibberishLanguage();

		List<String> words = lang.getWords(5000, 2, 7);
		assertTrue(words.size() > 500);

		List<String> sentences = IntStream.range(0, 10).mapToObj(i ->
			IntStream.range(5, 12).mapToObj(j ->
					words.get(Rng.intInRange(0, words.size()))
			).collect(Collectors.joining(" "))
		).collect(Collectors.toList());

		assertEquals(sentences.size(), 10);
		assertTrue(lang.isValidWord("loyal"));
		assertTrue(lang.isValidWord("murder"));
	}
	@Test
	public void saveAndRestoreLanguageFromSttFile() {
		Language gibberish = gibberishLanguage();
		assertTrue(FileUtils.writeToFile(testLanguage, gibberish.asTransitionTable()));
		assertTrue(FileUtils.fileExists(testLanguage));
		List<String> rows = FileUtils.getFileContents(testLanguage);
		Language restoredGibberish = Language.getLanguageFromStt(rows);

		assertEquals(gibberish, restoredGibberish);
	}

}
