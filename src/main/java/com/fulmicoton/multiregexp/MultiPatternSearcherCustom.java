/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fulmicoton.multiregexp;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import dk.brics.automaton.State;
import dk.brics.automaton.StatePair;
import dk.brics.automaton.Transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author namtv19
 */
public class MultiPatternSearcherCustom {

    private final MultiPatternAutomaton automaton;
    private final List<RunAutomaton> individualAutomatons;
    private final List<RunAutomaton> inverseAutomatons;

    MultiPatternSearcherCustom(final MultiPatternAutomaton automaton,
            final List<Automaton> individualAutomatons) {
        this.automaton = automaton;
        this.individualAutomatons = new ArrayList<>();
        for (final Automaton individualAutomaton : individualAutomatons) {
            this.individualAutomatons.add(new RunAutomaton(individualAutomaton));
        }
        this.inverseAutomatons = new ArrayList<>(this.individualAutomatons.size());
        for (final Automaton individualAutomaton : individualAutomatons) {
            final Automaton inverseAutomaton = inverseAutomaton(individualAutomaton);
            this.inverseAutomatons.add(new RunAutomaton(inverseAutomaton));
        }
    }

    public static MultiPatternAutomaton makeAutomatonWithPrefix(List<String> patterns, String prefix) {
        final List<Automaton> automata = new ArrayList<>();
        for (final String ptn : patterns) {
            final String prefixedPattern = prefix + ptn;
            final Automaton automaton = new RegExp(prefixedPattern).toAutomaton();
            automaton.minimize();
            automata.add(automaton);
        }
        return MultiPatternAutomaton.make(automata);
    }

    public static MultiPatternSearcherCustom searcher(String[] patterns) {
        return searcher(Arrays.asList(patterns));
    }

    /**
     * Equivalent of Pattern.compile, but the result is only valid for pattern
     * search. The searcher will return the first occurrence of a pattern.
     *
     * This operation is costly, make sure to cache its result when performing
     * search with the same patterns against the different strings.
     *
     * @param patterns
     * @return A searcher object
     */
    public static MultiPatternSearcherCustom searcher(List<String> patterns) {
//        final MultiPatternAutomaton searcherAutomaton = makeAutomatonWithPrefix(patterns, ".*");
        final MultiPatternAutomaton searcherAutomaton = makeAutomatonWithPrefix(patterns, "");
        final List<Automaton> indidivualAutomatons = new ArrayList<>();
        for (final String pattern : patterns) {
            final Automaton automaton = new RegExp(pattern).toAutomaton();
            automaton.minimize();
            automaton.determinize();
            indidivualAutomatons.add(automaton);
        }
        return new MultiPatternSearcherCustom(searcherAutomaton, indidivualAutomatons);
    }

    static Automaton inverseAutomaton(final Automaton automaton) {
        final Map<State, State> stateMapping = new HashMap<>();
        for (final State state : automaton.getStates()) {
            stateMapping.put(state, new State());
        }
        for (final State state : automaton.getStates()) {
            for (final Transition transition : state.getTransitions()) {
                final State invDest = stateMapping.get(state);
                final State invOrig = stateMapping.get(transition.getDest());
                invOrig.addTransition(new Transition(transition.getMin(), transition.getMax(), invDest));
            }
        }
        final Automaton inverseAutomaton = new Automaton();
        stateMapping.get(automaton.getInitialState()).setAccept(true);
        final State initialState = new State();
        inverseAutomaton.setInitialState(initialState);
        final List<StatePair> epsilons = new ArrayList<>();
        for (final State acceptState : automaton.getAcceptStates()) {
            final State invOrigState = stateMapping.get(acceptState);
            final StatePair statePair = new StatePair(initialState, invOrigState);
            epsilons.add(statePair);
        }
        inverseAutomaton.addEpsilons(epsilons);
        return inverseAutomaton;
    }

    public Cursor search(CharSequence s) {
        return search(s, 0);
    }

    public Cursor search(CharSequence s, int position) {
        return new Cursor(s, position);
    }

    public static List<Character> stringToCharacters(String in) {
        char[] chars = in.toCharArray();
        List<Character> items = new LinkedList<>();
        for (char aChar : chars) {
            items.add(aChar);
        }
        return items;
    }
    public static final String SPACE_BOUNDARY = " \r\n\t\f";
    public static final String PRE_BOUNDARY = "[{\"(<";
    public static final String POST_BOUNDARY = "]}\")>";
    public static final String SENTENCE_BOUNDARY = ".!?,:;|";
    static final HashSet<Character> SPACE_BOUNDARY_SET = new HashSet<>(stringToCharacters(SPACE_BOUNDARY));
    static final HashSet<Character> PRE_BOUNDARY_SET = new HashSet<>(stringToCharacters(PRE_BOUNDARY));
    static final HashSet<Character> POST_BOUNDARY_SET = new HashSet<>(stringToCharacters(POST_BOUNDARY));
    static final HashSet<Character> SENTENCE_BOUNDARY_SET = new HashSet<>(stringToCharacters(SENTENCE_BOUNDARY));

    public static boolean isBoundaryLeft(final int start, final CharSequence input) {
        //start = 0
        return (start == 0
                || (start > 0
                && (PRE_BOUNDARY_SET.contains(input.charAt(start - 1))
                || SPACE_BOUNDARY_SET.contains(input.charAt(start - 1))))
                || (start > 1 && SENTENCE_BOUNDARY_SET.contains(input.charAt(start - 1)) && SPACE_BOUNDARY_SET.contains(input.charAt(start - 2))));
    }

    public static boolean isBoundaryRight(final int end, final CharSequence input) {
        int pos1 = input.length() - 1;
        //start = 0
        return (end >= pos1
                || (end < pos1
                && (POST_BOUNDARY_SET.contains(input.charAt(end + 1))
                || SPACE_BOUNDARY_SET.contains(input.charAt(end + 1))))
                || (end == pos1 - 1 && SENTENCE_BOUNDARY_SET.contains(input.charAt(end + 1)))
                || (end < pos1 - 1 && SENTENCE_BOUNDARY_SET.contains(input.charAt(end + 1)) && SPACE_BOUNDARY_SET.contains(input.charAt(end + 2))));
    }

    public static boolean isBoundary(final int start, final int end, final CharSequence input) {
        //start = 0
        return isBoundaryLeft(start, input) && isBoundaryRight(end, input);
    }

    public class Cursor {

        private final CharSequence seq;
        private int matchingPattern = -1;
        private int end = 0;
        private int start = -1;

        List<Integer> matchings = new ArrayList<>(4);
        List<Integer> starts = new ArrayList<>(4);
        List<Integer> ends = new ArrayList<>(4);

        Cursor(CharSequence seq, int position) {
            this.seq = seq;
            this.end = position;
        }

        public int start() {
            return this.start;
        }

        public int end() {
            return this.end;
        }

        private void reset() {
            matchings = new ArrayList<>(4);
            starts = new ArrayList<>(4);
            ends = new ArrayList<>(4);
        }

        public List<Integer> getMatchings() {
            return matchings;
        }

        public List<Integer> getStarts() {
            return starts;
        }

        public List<Integer> getEnds() {
            return ends;
        }

        public int match() {
            return this.matchingPattern;
        }

        public boolean found() {
            return this.matchingPattern >= 0;
        }


        /* Advances the cursor, to the next match of any pattern.
         * Matches returned cannot overlap.
         *
         * Any ambiguity is solved according to the following method.
         *
         * 1) we advance up to the end of at least one pattern
         * 2) if more than one pattern is found choose the one the highest
         * priority (== lower id)
         * 3) we choose the leftmost possible start for this pattern
         * to match at the end we found.
         * 4) Finally, we extend the pattern as much as possible on the right.
         *
         * The function then returns true and start(), end() will
         * return respectively the starting offset of the pattern.
         * position holds the offset of what would
         * be the character right after the match.
         *
         * If no match is found the function return false.
         */
        public boolean next() {
            this.start = -1;
            this.matchingPattern = -1;
            reset();
            int[] matches = new int[0];

            final int seqLength = this.seq.length();
            if (this.end >= seqLength) {
                return false;
            }

            { // first find a match and "choose the pattern".
                int state = 0;
                boolean bInit = false;
                boolean bNextPos = true;
                int pos = this.end;
                int nextPos = -1;
                for (; pos < seqLength; pos++) {
                    final char c = this.seq.charAt(pos);

                    state = automaton.step(state, c);
                    if (state == -1) {
                        state = 0;
                        if (bInit) {
                            break;
                        } else {
                            this.start = pos;
                            this.end = pos;
                            bNextPos = true;
                            continue;
                        }
                    } else if (!bInit) {
                        //First Found
                        bInit = true;
                    }

                    if (automaton.atLeastOneAccept[state]) {
//                    if (automaton.atLeastOneAccept[state] && automaton.accept[state].length > 0 && isBoundaryRight(pos, seq)) {
                        // We found a match!
                        matches = automaton.accept[state];
                        this.matchingPattern = matches[0];
                        this.end = pos;
//                        break;
                        bNextPos = true;
                    } else if (bNextPos) {
                        //Kiem tra xem co bat dau boi 1 regex ko
                        int stt = automaton.step(0, c);
                        if (automaton.hasManyState(stt)) {
                            bNextPos = false;
                            nextPos = pos;
                        }
                    }
                }
                if (this.matchingPattern == -1) {
                    if (bNextPos) {
                        this.end = this.end + 1;
                    } else {
                        this.end = nextPos + 1;
                    }
                    return next();
//                    return false;
                }
            }
            int beginPos = this.end;
            int beginStart = this.start;
            boolean found = false;

            List<Integer> matcheds = new LinkedList<>();
            List<Integer> starteds = new LinkedList<>();
            List<Integer> endeds = new LinkedList<>();

//            HashSet<Integer> items = new HashSet<>();
//            for (Integer matched : matches) {
//                items.add(matched);
//            }

            for (int in : matches) {
                this.matchingPattern = in;
                {   // we rewind using the backward automaton to find the start of the pattern.
                    final RunAutomaton backwardAutomaton = inverseAutomatons.get(this.matchingPattern);
                    int state = backwardAutomaton.getInitialState();
                    for (int pos = this.end; pos >= 0; pos--) {
                        final char c = this.seq.charAt(pos);
                        state = backwardAutomaton.step(state, c);
                        if (state == -1) {
                            break;
                        }
                        if (backwardAutomaton.isAccept(state)) {
                            start = pos;
                        }
                    }
                }

                {   // we go forward again using the forward automaton to find the end of the pattern.
                    final RunAutomaton forwardAutomaton = individualAutomatons.get(this.matchingPattern);
                    int state = forwardAutomaton.getInitialState();
                    for (int pos = this.start; pos < seqLength; pos++) {
                        final char c = this.seq.charAt(pos);
                        state = forwardAutomaton.step(state, c);
                        if (state == -1) {
                            break;
                        }
                        if (forwardAutomaton.isAccept(state)) {
                            this.end = pos + 1;
                        }
                    }
                }
//                if (true || isBoundary(start, end - 1, seq)) {
                found = true;
//                    break;
                matcheds.add(this.matchingPattern);
                starteds.add(start);
                endeds.add(end);
//                }
                this.end = beginPos;
                this.start = beginStart;
            }
            if (!found) {
                this.end = beginPos + 1;
                return next();
            }
            this.end = beginPos + 1;
            this.matchings = matcheds;
            this.starts = starteds;
            this.ends = endeds;
            return true;
        }

    }
}
