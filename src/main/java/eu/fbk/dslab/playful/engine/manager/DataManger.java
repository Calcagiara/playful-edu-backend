package eu.fbk.dslab.playful.engine.manager;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import eu.fbk.dslab.playful.engine.model.Activity;
import eu.fbk.dslab.playful.engine.model.Concept;
import eu.fbk.dslab.playful.engine.model.Educator;
import eu.fbk.dslab.playful.engine.model.ExternalActivity;
import eu.fbk.dslab.playful.engine.model.Learner;
import eu.fbk.dslab.playful.engine.model.LearningScenario;
import eu.fbk.dslab.playful.engine.repository.ActivityRepository;
import eu.fbk.dslab.playful.engine.repository.CompetenceRepository;
import eu.fbk.dslab.playful.engine.repository.ConceptRepository;
import eu.fbk.dslab.playful.engine.repository.EducatorRepository;
import eu.fbk.dslab.playful.engine.repository.ExternalActivityRepository;
import eu.fbk.dslab.playful.engine.repository.LearnerRepository;
import eu.fbk.dslab.playful.engine.repository.LearningScenarioRepository;

@Service
public class DataManger {
	private static transient final Logger logger = LoggerFactory.getLogger(DataManger.class);
	
	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	LearningScenarioRepository learningScenarioRepository;
	@Autowired
	LearnerRepository learnerRepository;
	@Autowired
	EducatorRepository educatorRepository;
	@Autowired
	ConceptRepository conceptRepository;
	@Autowired
	ActivityRepository activityRepository;
	@Autowired
	ExternalActivityRepository externalActivityRepository;
	@Autowired
	CompetenceRepository competenceRepository;	
	
	public Page<Learner> getLearnerScenario(String domainId, String learningScenarioId, 
			String text, Pageable pageRequest) {
		Criteria criteria = new Criteria("domainId").is(domainId);
		
        if(StringUtils.isNotBlank(learningScenarioId)) {
        	LearningScenario scenario = learningScenarioRepository.findById(learningScenarioId).orElse(null);
        	if(scenario != null) {
        		criteria = criteria.and("id").in(scenario.getLearners());
        	}
        }
        
        if(StringUtils.isNotBlank(text)) {
        	criteria = criteria.orOperator(Criteria.where("email").regex(text, "i"), Criteria.where("nickname").regex(text, "i"),
        			Criteria.where("firstname").regex(text, "i"), Criteria.where("lastname").regex(text, "i"));
        }
        
        Query query = new Query(criteria);
        List<Learner> result = mongoTemplate.find(query, Learner.class);
		return new PageImpl<>(result, pageRequest, result.size());
	}
	
	public Concept removeConcept(String id) {
		Concept concept = conceptRepository.findById(id).orElse(null);
		if(concept != null) {
			//remove from activities
			List<Activity> activities = activityRepository.findByDomainIdAndGoals(concept.getDomainId(), id);
			activities.forEach(a -> {
				if(a.getGoals().contains(id)) {
					a.getGoals().remove(id);
					activityRepository.save(a);
				}
			});
			
			//remove from ext activities
			List<ExternalActivity> extActivities = externalActivityRepository.findByDomainIdAndConceptId(concept.getDomainId(), id);
			extActivities.forEach(a -> {
				boolean save = false;
				if(a.getPreconditions().contains(id)) {
					a.getPreconditions().remove(id);
					save = true;
				}
				if(a.getEffects().contains(id)) {
					a.getEffects().remove(id);
					save = true;
				}
				if(save) {
					externalActivityRepository.save(a);
				}
			});
	
			conceptRepository.deleteById(id);
		}
		return concept;
	}
	
	public Learner removeLearner(String id) {
		Learner learner = learnerRepository.findById(id).orElse(null);
		if(learner != null) {
			//remove from learning scenario
			List<LearningScenario> list = learningScenarioRepository.findByDomainIdAndLearners(learner.getDomainId(), id);
			list.forEach(ls -> {
				if(ls.getLearners().contains(id)) {
					ls.getLearners().remove(id);
					learningScenarioRepository.save(ls);
				}
			});
		}
		return learner;
	}
	
	public Educator removeEducator(String id) {
		Educator educator = educatorRepository.findById(id).orElse(null);
		if(educator != null ) {
			//remove from learning scenario
			List<LearningScenario> list = learningScenarioRepository.findByDomainIdAndEducators(educator.getDomainId(), id);
			list.forEach(ls -> {
				if(ls.getEducators().contains(id)) {
					ls.getEducators().remove(id);
					learningScenarioRepository.save(ls);
				}
			});
		}
		return educator;
	}
}
