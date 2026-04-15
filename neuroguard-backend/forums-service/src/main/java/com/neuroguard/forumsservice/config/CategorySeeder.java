package com.neuroguard.forumsservice.config;

import com.neuroguard.forumsservice.entity.Category;
import com.neuroguard.forumsservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    private static final List<String> DEFAULT_NAMES = List.of(
            "General", "Medication", "Caregiving", "Symptoms", "Support", "Resources"
    );

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) return;
        for (String name : DEFAULT_NAMES) {
            Category c = new Category();
            c.setName(name);
            categoryRepository.save(c);
        }
    }
}
