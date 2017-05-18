import opennlp.tools.ngram.NGramGenerator;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by cstaheli on 5/16/2017.
 */
public class Driver
{
    private SortedSet<Map.Entry<String, Integer>> allWordsSortedByFrequency;
    private SortedSet<Map.Entry<String, Integer>> allBiGramsSortedByFrequency;
    private SortedSet<Map.Entry<String, Integer>> allCombinations;
    private List<Map.Entry<Integer, Integer>> vocabSizeList;
    private int totalWordsEncountered;
    private Set<String> uniqueWords;

    public Driver()
    {
        totalWordsEncountered = 0;
        uniqueWords = new HashSet<>();
        vocabSizeList = new ArrayList<>();
    }

    public static void main(String[] args)
    {
        Driver driver = new Driver();
        driver.readInCorpus("src/main/resources");
        driver.outputResults("src/main/resources/output/");
    }

    private static <K extends Comparable<? super K>, V extends Comparable<? super V>>
    SortedSet<Map.Entry<K, V>> entriesSortedByReverseValues(Map<K, V> map)
    {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<>(
                (e1, e2) ->
                {
                    int valueComparison = e2.getValue().compareTo(e1.getValue());
                    int keyComparison = e1.getKey().compareTo(e2.getKey());
                    return valueComparison != 0 ? valueComparison : keyComparison;
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    private void outputResults(String directory)
    {
        try
        {
            new File(directory + "words.csv").delete();
            new File(directory + "biGrams.csv").delete();
            new File(directory + "combos.csv").delete();
            new File(directory + "vocabGrowth.csv").delete();

            PrintWriter words = new PrintWriter(directory + "words.csv", "UTF-8");
            outputRankFrequency(allWordsSortedByFrequency, words);
            PrintWriter biGrams = new PrintWriter(directory + "biGrams.csv", "UTF-8");
            outputRankFrequency(allBiGramsSortedByFrequency, biGrams);
            PrintWriter combos = new PrintWriter(directory + "combos.csv", "UTF-8");
            outputRankFrequency(allCombinations, combos);
            PrintWriter vocabGrowth = new PrintWriter(directory + "vocabGrowth.csv", "UTF-8");
            outputVocabGrowth(vocabSizeList, vocabGrowth);
        }
        catch (FileNotFoundException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    private void outputVocabGrowth(List<Map.Entry<Integer, Integer>> vocabList, PrintWriter writer)
    {
        writer.append("Total Words,Total Unique Words\n");
        for (Map.Entry<Integer, Integer> entry : vocabList)
        {
            writer.append(String.format("%s,%s\n", entry.getKey(), entry.getValue()));
        }
    }

    private void outputRankFrequency(SortedSet<Map.Entry<String, Integer>> set, PrintWriter writer)
    {
        int counter = 0;
        writer.append("Word,Log Rank,Log Frequency\n");
        for (Map.Entry<String, Integer> entry : set)
        {
            writer.append(String.format("%s,%s,%s\n", entry.getKey(), Math.log10(++counter), Math.log10(entry.getValue())));
        }
    }

    private void readInCorpus(String directory)
    {
        Map<String, Integer> allWords = new TreeMap<>();
        Map<String, Integer> allBiGrams = new TreeMap<>();
        Map<String, Integer> allCombos = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directory)))
        {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .filter(path -> path.toString().contains("Doc"))
                    .forEach(filePath ->
                    {
                        String fileName = filePath.toString();
                        List<String> words = readFile(fileName);
                        List<String> biGrams = NGramGenerator.generate(words, 2, " ");
                        addWordCounts(words);
                        allWords.putAll(mappedValueFrequencies(allWords, words));
                        allBiGrams.putAll(mappedValueFrequencies(allBiGrams, biGrams));
                    });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        allWordsSortedByFrequency = entriesSortedByReverseValues(allWords);
        allBiGramsSortedByFrequency = entriesSortedByReverseValues(allBiGrams);
        allCombos.putAll(allWords);
        allCombos.putAll(allBiGrams);
        allCombinations = entriesSortedByReverseValues(allCombos);
    }

    private void addWordCounts(List<String> words)
    {
        for (String word : words)
        {
            ++totalWordsEncountered;
            uniqueWords.add(word);
            Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<>(totalWordsEncountered, uniqueWords.size());
            vocabSizeList.add(entry);
        }
    }

    private List<String> readFile(String fileName)
    {
        //A lot of the files aren't encoded with UTF-8
        try
        {
            String file = new String(Files.readAllBytes(Paths.get(fileName)), Charset.forName("ISO-8859-1"));
            file = file
                    .toLowerCase()
                    .replaceAll("[^\\w]+", " ")
                    .replaceAll("[0-9]", "");
            Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
            String[] tokens = tokenizer.tokenize(file);
            return Arrays.asList(tokens);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private Map<String, Integer> mappedValueFrequencies(Map<String, Integer> existingMap, List<String> values)
    {
        if (existingMap == null)
        {
            existingMap = new TreeMap<>();
        }
        for (String value : values)
        {
            Integer count = existingMap.get(value);
            existingMap.put(value, (count == null) ? 1 : count + 1);
        }
        return existingMap;
    }
}
