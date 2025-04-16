package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AhoCorasickMatcher {
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

    public void initialize(List<FilteringWord> filteringWords) {
        root = new Node();

        // 트라이 구축
        for (FilteringWord word : filteringWords) {
            if (!word.isActive()) continue;;

            Node current = root;
            String pattern = word.getWord();

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

    // todo: 필터링 시 모든 단어에 대해 탐색할건지 or 필터링 단어 하나만 걸려도 바로 반환할건지
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
