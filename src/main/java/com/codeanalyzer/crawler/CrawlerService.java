package com.codeanalyzer.crawler;

import com.codeanalyzer.model.Student;

public class CrawlerService {
    private final CodeforceCrawler codeforceCrawler = new CodeforceCrawler();

    public void startCrawl(Student student) {
        if (student.getPlatform() == com.codeanalyzer.model.PlatformType.CODEFORCES) {
            codeforceCrawler.crawlForStudent(student);
        } else {
            System.out.println("Nền tảng " + student.getPlatform() + " hiện chưa được hỗ trợ crawl.");
        }
    }
}
