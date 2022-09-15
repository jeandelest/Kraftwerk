package fr.insee.kraftwerk.core.extradata.paradata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ParaDataUE {
  private Path filepath;
  
  private String identifier;
  
  private List<Event> events = new ArrayList<>();
  
  private Map<String, List<ParadataVariable>> paraDataVariables = new LinkedHashMap<>();
  
  private List<ParadataOrchestrator> paraDataOrchestrators = new ArrayList<>();
  
  private List<ParadataSession> paraDataSessions = new ArrayList<>();
  
  private List<Session> sessions = new ArrayList<>();
  
  private List<Orchestrator> orchestrators = new ArrayList<>();
  
  public Path getFilepath() {
    return this.filepath;
  }
  
  public void setFilepath(Path filepath) {
    this.filepath = filepath;
  }
  
  public String getIdentifier() {
    return this.identifier;
  }
  
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
  
  public List<Event> getEvents() {
    return this.events;
  }
  
  public void setEvents(List<Event> events) {
    this.events = events;
  }
  
  public Map<String, List<ParadataVariable>> getParadataVariables() {
    return this.paraDataVariables;
  }
  
  public List<ParadataVariable> getParadataVariable(String variableName) {
    return this.paraDataVariables.get(variableName);
  }
  
  public void setParadataVariables(Map<String, List<ParadataVariable>> paraDataVariables) {
    this.paraDataVariables = paraDataVariables;
  }
  
  public void addParadataVariable(ParadataVariable paraDataVariable) {
    String variableName = paraDataVariable.getVariableName();
    if (this.paraDataVariables.containsKey(variableName)) {
      this.paraDataVariables.get(variableName).add(paraDataVariable);
    } else {
      this.paraDataVariables.put(variableName, new ArrayList<>());
      this.paraDataVariables.get(variableName).add(paraDataVariable);
    } 
  }
  
  public List<ParadataOrchestrator> getParadataOrchestrators() {
    return this.paraDataOrchestrators;
  }
  
  public void setParadataOrchestrators(List<ParadataOrchestrator> paraDataOrchestrators) {
    this.paraDataOrchestrators = paraDataOrchestrators;
  }
  
  public void addParadataOrchestrator(ParadataOrchestrator paraDataOrchestrator) {
    this.paraDataOrchestrators.add(paraDataOrchestrator);
  }
  
  public List<ParadataSession> getParadataSessions() {
    return this.paraDataSessions;
  }
  
  public void setParadataSessions(List<ParadataSession> paraDataSessions) {
    this.paraDataSessions = paraDataSessions;
  }
  
  public void addParadataSession(ParadataSession paraDataSession) {
    this.paraDataSessions.add(paraDataSession);
  }
  
  public List<Session> getSessions() {
    return this.sessions;
  }
  
  public void setSessions(List<Session> sessions) {
    this.sessions = sessions;
  }
  
  public void addSession(Session session) {
    this.sessions.add(session);
  }
  
  public List<Orchestrator> getOrchestrators() {
    return this.orchestrators;
  }
  
  public void setOrchestrators(List<Orchestrator> orchestrators) {
    this.orchestrators = orchestrators;
  }
  
  public void addOrchestrator(Orchestrator orchestrator) {
    this.orchestrators.add(orchestrator);
  }

  
  public void sortEvents() {
    this.setEvents(this.getEvents()
        .stream()
        .distinct()
        .sorted(Comparator.comparingLong(Event::getTimestamp).thenComparing(Event::getIdParadataObject))
        .collect(Collectors.toList()));
  }
  
  public long createLengthOrchestratorsVariable() {
    long result = 0L;
    for (Orchestrator orchestrator : getOrchestrators())
      result += orchestrator.getValidation() - orchestrator.getInitialization(); 
    return result;
  }
  
  public void createOrchestratorsAndSessions() {
    List<Event> listParadataEvents = getEvents();
    Session session = new Session("Initialization ongoing", 0L, 0L);
    Orchestrator orchestrator = new Orchestrator(identifier, 0L, 0L);
    if (!listParadataEvents.isEmpty()) {
      for (int j = 0; j < listParadataEvents.size() - 1; j++) {
        Event event1 = listParadataEvents.get(j);
        Event previousEvent;
        if (session.getIdentifier().contentEquals("Initialization ongoing"))
          session.setIdentifier(event1.getIdSession()); 
        if (event1.getIdParadataObject().contentEquals("init-session")) {
          if (session.getInitialization() == 0L) {
            session.setInitialization(event1.getTimestamp());
          } else {
            previousEvent = listParadataEvents.get(j - 1);
            session.setTermination(previousEvent.getTimestamp());
            addSession(session);
            session = new Session(event1.getIdSession(), event1.getTimestamp(), 0L);
            if (orchestrator.getInitialization() != 0L && 
              orchestrator.getInitialization() < previousEvent.getTimestamp()) {
              orchestrator.setValidation(previousEvent.getTimestamp());
              addOrchestrator(orchestrator);
              orchestrator = new Orchestrator(identifier, 0L, 0L);
            } 
          } 
        }
        if (orchestrator.getInitialization() == 0L && 
          event1.getIdParadataObject().contentEquals("init-orchestrator-collect")) {
          orchestrator.setInitialization(event1.getTimestamp());
        } else if (orchestrator.getInitialization() != 0L && 
          event1.getIdParadataObject().contentEquals("init-orchestrator-collect")) {
          Session previousSession = getSessions().get(getSessions().size() - 1);
          if (previousSession.getTermination() != orchestrator.getInitialization() && orchestrator.getInitialization() != 0L) {
              previousEvent = listParadataEvents.get(j - 1);
              addOrchestrator(new Orchestrator(identifier, orchestrator.getInitialization(), 
                    previousEvent.getTimestamp()));
              orchestrator.setInitialization(event1.getTimestamp());
          }
        } else if (event1.getIdParadataObject().contentEquals("validate-button-orchestrator-collect")) {
          previousEvent = listParadataEvents.get(j - 1);
          if (orchestrator.getInitialization() == 0L) {
            if (getSessions().isEmpty()) {
              orchestrator.setInitialization(session.getInitialization());
            } else {
              orchestrator.setInitialization((
                  getSessions().get(getSessions().size() - 1)).getInitialization());
            }  
          }
          if (orchestrator.getInitialization() < previousEvent.getTimestamp()) {
            orchestrator.setValidation(event1.getTimestamp());
            addOrchestrator(orchestrator);
          } 
          orchestrator = new Orchestrator(identifier, 0L, 0L);
        } 
      } 
      Event event = listParadataEvents.get(listParadataEvents.size() - 1);
      if (session.getInitialization() != 0L && session.getTermination() == 0L) {
        session.setTermination(event.getTimestamp());
        addSession(session);
      } 
      if (orchestrator.getInitialization() != 0L && orchestrator.getValidation() == 0L && 
        orchestrator.getInitialization() < event.getTimestamp()) {
        orchestrator.setValidation(event.getTimestamp());
        addOrchestrator(orchestrator);
      } 
    } 
  }
}
