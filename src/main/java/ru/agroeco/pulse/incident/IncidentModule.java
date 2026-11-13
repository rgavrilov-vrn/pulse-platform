import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Incident",
        allowedDependencies = {"platform", "auth", "audit", "videoserver::api"}
)
public interface IncidentModule {}