package com.mindmate.mindmate_server.chat.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AhoCorasickMatcher<T> {

    public interface MatchableItem {
        String getPattern();
        boolean isActive();
    }

    private static class Node{
        Map<Character, Node> children = new HashMap<>();
        Node failureLink;
        List<String> outputs = new ArrayList<>();

        public void addOutput(String word) {
            outputs.add(word);
        }
    }

    private Node root;
    private boolean initialized = false;
    private Map<String, T> itemMap = new HashMap<>();

    public void initialize(List<T> items, Function<T, String> patternExtractor, Function<T, Boolean> activeChecker) {
        root = new Node();
        itemMap.clear();

        // 트라이 구축
        for (T item : items) {
            if (!activeChecker.apply(item)) continue;

            String pattern = patternExtractor.apply(item);
            itemMap.put(pattern, item);

            Node current = root;
            for (char c : pattern.toCharArray()) {
                current.children.putIfAbsent(c, new Node());
                current = current.children.get(c);
            }

            current.addOutput(pattern);
        }

        // 실패 링크 구축 -> BFS
        Queue<Node> queue = new LinkedList<>();

        for (Node child : root.children.values()) {
            child.failureLink = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
                char c = entry.getKey();
                Node child = entry.getValue();
                queue.add(child);

                // 실패 링크 찾기
                Node failureNode = current.failureLink;
                while (failureNode != null && !failureNode.children.containsKey(c)) {
                    failureNode = failureNode.failureLink;
                }

                if (failureNode == null) {
                    child.failureLink = root;
                } else {
                    child.failureLink = failureNode.children.get(c);
                    child.outputs.addAll(child.failureLink.outputs);
                }
            }
        }

        initialized = true;
    }

    public List<T> searchItems(String text) {
        List<String> patterns = search(text);
        return patterns.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> search(String text) {
        if (!initialized) {
            throw new IllegalStateException("Aho-Corasick matcher is not initialized");
        }

        List<String> results = new ArrayList<>();
        Node current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 현재 상태에서 다음 문자로 전이할 수 없는 경우 실패 링크를 따라감
            while (current != root && !current.children.containsKey(c)) {
                current = current.failureLink;
            }

            // 다음 상태로 전이
            if (current.children.containsKey(c)) {
                current = current.children.get(c);
            }

            // 현재 상태에 출력이 있으면 결과에 추가
            if (!current.outputs.isEmpty()) {
                results.addAll(current.outputs);
            }
        }

        return results;
    }

    public Optional<T> findFirstMatchItem(String text) {
        Optional<String> pattern = findFirstMatch(text);
        return pattern.map(itemMap::get);
    }

    public Optional<String> findFirstMatch(String text) {
        if (!initialized) {
            throw new IllegalStateException("Aho-Corasick matcher is not initialized");
        }

        Node current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (current != root && !current.children.containsKey(c)) {
                current = current.failureLink;
            }

            if (current.children.containsKey(c)) {
                current = current.children.get(c);
            }

            if (!current.outputs.isEmpty()) {
                return Optional.of(current.outputs.get(0));
            }
        }
        return Optional.empty();
    }
}
