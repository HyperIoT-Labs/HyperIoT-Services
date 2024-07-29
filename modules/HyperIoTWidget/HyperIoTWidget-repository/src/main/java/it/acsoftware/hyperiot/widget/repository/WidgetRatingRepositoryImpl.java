/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.widget.repository;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.widget.api.WidgetRatingRepository;
import it.acsoftware.hyperiot.widget.model.Widget;
import it.acsoftware.hyperiot.widget.model.WidgetRating;
import org.apache.aries.jpa.template.JpaTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * @author Aristide Cittadino Implementation class of the Widget. This
 * class is used to interact with the persistence layer.
 */
@Component(service = WidgetRatingRepository.class, immediate = true)
public class WidgetRatingRepositoryImpl extends HyperIoTBaseRepositoryImpl<WidgetRating> implements WidgetRatingRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;
    private HUserRepository huserRepository;

    /**
     *
     * @param huserRepository
     */
    @Reference
    protected void setRepository(HUserRepository huserRepository) {
        getLog().debug( "invoking setRepository, setting: {}", huserRepository);
        this.huserRepository = huserRepository;
    }

    /**
     * Constructor for a WidgetRepositoryImpl
     */
    public WidgetRatingRepositoryImpl() {
        super(WidgetRating.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().debug( "invoking getJpa, returning: {}" , jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-widget-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().debug( "invoking setJpa, setting: {}" , jpa);
        this.jpa = jpa;
    }

    @Override
    public void rateWidget(int rating, Widget w, String loggedUsername) {
        final HUser u = huserRepository.findByUsername(loggedUsername);
        if (u != null) {
            if (rating < 1)
                rating = 1;
            if (rating > 5)
                rating = 5;
            int finalRating = rating;
            jpa.tx(entityManager -> {
                Query q = entityManager.createQuery("from WidgetRating wr where wr.user.id = :userId and wr.widget.id = :widgetId");
                q.setParameter("userId", u.getId());
                q.setParameter("widgetId", w.getId());
                WidgetRating r = null;
                boolean isNew = false;
                try {
                    r = (WidgetRating) q.getSingleResult();
                } catch (NoResultException e) {
                    r = new WidgetRating();
                    isNew = true;
                }
                r.setRating(finalRating);
                r.setUser(u);
                r.setWidget(w);
                if (isNew)
                    entityManager.persist(r);
                else
                    entityManager.merge(r);
                entityManager.flush();
            });
            return;
        }
        throw new HyperIoTEntityNotFound();
    }
}
