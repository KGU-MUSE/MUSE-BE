package muse.domain.support;

import jakarta.persistence.*;

@Entity
@Table(name = "domain_wiring_probe")
public class DomainProbe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    protected DomainProbe() {}

    public DomainProbe(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
