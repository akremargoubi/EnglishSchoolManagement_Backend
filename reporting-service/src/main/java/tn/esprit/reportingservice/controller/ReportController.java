
package tn.esprit.reportingservice.controller;


import tn.esprit.reportingservice.entity.Report;
import tn.esprit.reportingservice.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")

public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping
    public Report create(@RequestBody Report r) {
        return service.create(r);
    }

    @GetMapping
    public List<Report> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Report getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Report update(@PathVariable Long id, @RequestBody Report r) {
        return service.update(id, r);
    }

    @PatchMapping("/{id}/status")
    public Report updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return service.updateStatus(id, payload.getOrDefault("status", "IN_PROGRESS"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
