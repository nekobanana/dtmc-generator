package com.github.nekobanana.dtmcgenerator.sampling.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nekobanana.dtmcgenerator.sampling.sampler.PerfectSampler;
import com.github.nekobanana.dtmcgenerator.sampling.sampler.RunResult;
import com.github.nekobanana.dtmcgenerator.sampling.test.StatisticalTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerfectSampleRunner implements SamplerRunner {
    private PerfectSampler sampler;
    private List<RunResult> results = new ArrayList<>();
    private Double avgSteps;
    private Double stdDevSteps;
    private int minSamplesNumber = 2;
    private StatisticalTest stopConditionTest;

    private static final String postprocDirPath = "postprocess/";
    private static final String outputDirPath = postprocDirPath + "results/";

    public PerfectSampleRunner(PerfectSampler sampler, StatisticalTest stopConditionTest) {
        this.sampler = sampler;
        this.stopConditionTest = stopConditionTest;
    }

    @Override
    public void run() {
        avgSteps = null;
        stdDevSteps = null;
        for (int i = 0; i < minSamplesNumber; i++) {
            sampler.reset();
            RunResult result = sampler.runUntilCoalescence();
            results.add(result);
            stopConditionTest.addNewSample(result.getSteps());
        }
        do {
            sampler.reset();
            RunResult result = sampler.runUntilCoalescence();
            results.add(result);
            stopConditionTest.addNewSample(result.getSteps());
        } while (!stopConditionTest.test());
//        return results.size();
    }

    public Map<Integer, Double> getStatesDistribution() {return getStatesDistribution(false);}
    public Map<Integer, Double> getStatesDistribution(boolean print) {
        Map<Integer, Double> pi = SamplerRunner.getDistrFromResults(results, RunResult::getSampledState)
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e ->  (double)e.getValue() / results.size()));
        if (print) {
            System.out.println("\nPerfect sampling");
            pi.forEach((state, count) ->
                    System.out.println("state " + state + ": " + count));
        }
        return pi;
    }

    @Override
    public Map<Integer, Long> getStepsDistribution() {return getStepsDistribution(false);}
    public Map<Integer, Long> getStepsDistribution(boolean print) {
        Map<Integer, Long> hist = SamplerRunner.getDistrFromResults(results, RunResult::getSteps);
        if (print) {
            System.out.println("\nPerfect sampling");
            hist.forEach((state, count) ->
                    System.out.println("steps: " + state + ", count: " + count));
        }
        return hist;
    }

    @Override
    public Double getAvgSteps() {
            avgSteps = results.stream().mapToDouble(RunResult::getSteps).sum() / results.size();
        return avgSteps;
    }

    @Override
    public Double getStdDevSteps() {
            stdDevSteps = Math.sqrt(results.stream()
                    .mapToDouble(r -> Math.pow(r.getSteps() - avgSteps, 2)).sum() / (results.size() - 1));
        return stdDevSteps;
    }

    public void writeSequenceOutput(String dirName) throws IOException {
        Files.createDirectories(Paths.get(outputDirPath + dirName));
        String outputFileName = outputDirPath + dirName + "/last_seq.json";
        sampler.writeSequenceToFile(outputFileName);
    }

    public void writeResultsOutput(String dirName) throws IOException {
        Files.createDirectories(Paths.get(outputDirPath + dirName));
        String outputFileName = outputDirPath + dirName + "/results.json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        writer.write((new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(results));
        writer.close();
    }



    public int getNRuns() {
        return results.size();
    }
}
