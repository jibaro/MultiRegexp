package com.fulmicoton.multiregexp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.DkBricsAutomatonHelper;
import dk.brics.automaton.State;

public class MultiPatternAutomaton {

    final MultiState[] indexMultiState;
    public final int[][] accept;
    final boolean[] atLeastOneAccept;
    private final int stride;
    private final int[] transitions;
    private final int[] alphabet;
    private final int nbPatterns;
    int[] matcheds;

    private MultiPatternAutomaton(final int[][] accept,
            final int[] transitions,
            final char[] points,
            final int nbPatterns) {
        this.accept = accept;
        this.transitions = transitions;
        this.alphabet = alphabet(points);
        this.stride = points.length;
        this.atLeastOneAccept = new boolean[accept.length];
        for (int i = 0; i < accept.length; i++) {
            this.atLeastOneAccept[i] = this.accept[i].length > 0;
        }
        this.nbPatterns = nbPatterns;
        this.indexMultiState = new MultiState[0];
        makeCountState(points);
    }

    private MultiPatternAutomaton(final int[][] accept,
            final int[] transitions,
            final char[] points,
            final int nbPatterns, final MultiState[] indexMultiState) {
        this.accept = accept;
        this.transitions = transitions;
        this.alphabet = alphabet(points);
        this.stride = points.length;
        this.atLeastOneAccept = new boolean[accept.length];
        for (int i = 0; i < accept.length; i++) {
            this.atLeastOneAccept[i] = this.accept[i].length > 0;
        }
        this.nbPatterns = nbPatterns;
        this.indexMultiState = indexMultiState;
        makeCountState(points);
    }

    private void makeCountState(final char[] points) {
        matcheds = new int[this.accept.length];
        for (int i = 0; i < matcheds.length; i++) {
            for (State state : indexMultiState[i].states) {
                if (state != null) {
                    this.matcheds[i] = this.matcheds[i] + 1;
                }
            }
        }
    }

    static int[] alphabet(final char[] points) {
        final int[] alphabet = new int[Character.MAX_VALUE - Character.MIN_VALUE + 1];
        int i = 0;
        for (int j = 0; j <= (Character.MAX_VALUE - Character.MIN_VALUE); j++) {
            if (i + 1 < points.length && j == points[i + 1]) {
                i++;
            }
            alphabet[j] = i;
        }
        return alphabet;
    }

    static MultiState initialState(List<Automaton> automata) {
        final State[] initialStates = new State[automata.size()];
        int c = 0;
        for (final Automaton automaton : automata) {
            initialStates[c] = automaton.getInitialState();
            c += 1;
        }
        return new MultiState(initialStates);
    }

    static MultiPatternAutomaton make(final List<Automaton> automata) {
        for (final Automaton automaton : automata) {
            automaton.determinize();
        }

        final char[] points = DkBricsAutomatonHelper.pointsUnion(automata);

        // states that are still to be visited
        final Queue<MultiState> statesToVisits = new LinkedList<>();
        final MultiState initialState = initialState(automata);
        statesToVisits.add(initialState);

        final List<int[]> transitionList = new ArrayList<>();

        final Map<MultiState, Integer> multiStateIndex = new HashMap<>();
        multiStateIndex.put(initialState, 0);

        while (!statesToVisits.isEmpty()) {
            final MultiState visitingState = statesToVisits.remove();
            assert multiStateIndex.containsKey(visitingState);
            final int[] curTransitions = new int[points.length];
            for (int c = 0; c < points.length; c++) {
                final char point = points[c];
                final MultiState destState = visitingState.step(point);
                if (destState.isNull()) {
                    curTransitions[c] = -1;
                } else {
                    final int destStateId;
                    if (!multiStateIndex.containsKey(destState)) {
                        statesToVisits.add(destState);
                        destStateId = multiStateIndex.size();
                        multiStateIndex.put(destState, destStateId);
                    } else {
                        destStateId = multiStateIndex.get(destState);
                    }
                    curTransitions[c] = destStateId;
                }
            }
            transitionList.add(curTransitions);
        }

        assert transitionList.size() == multiStateIndex.size();
        final int nbStates = multiStateIndex.size();

        final int[] transitions = new int[nbStates * points.length];
        for (int stateId = 0; stateId < nbStates; stateId++) {
//            for (int pointId = 0; pointId < points.length; pointId++) {
//                transitions[stateId * points.length + pointId] = transitionList.get(stateId)[pointId];
//            }
            System.arraycopy(transitionList.get(stateId), 0, transitions, stateId * points.length, points.length);
        }

        final MultiState[] indexMultiState = new MultiState[nbStates];
        final int[][] acceptValues = new int[nbStates][];
        for (final Map.Entry<MultiState, Integer> entry : multiStateIndex.entrySet()) {
            final Integer stateId = entry.getValue();
            final MultiState multiState = entry.getKey();
            acceptValues[stateId] = multiState.toAcceptValues();
            indexMultiState[stateId] = multiState;
        }

        return new MultiPatternAutomaton(acceptValues, transitions, points, automata.size(), indexMultiState);
    }

    public int step(final int state, final char c) {
        return transitions[(state * this.stride) + alphabet[c - Character.MIN_VALUE]];
    }

    public int getNbPatterns() {
        return this.nbPatterns;
    }

    public boolean hasManyState(final int state) {
        if (state != -1) {
            return this.matcheds[state] > 1;
        }
        return false;
    }
}
