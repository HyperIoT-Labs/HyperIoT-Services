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

package it.acsoftware.hyperiot.algorithm.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.base.validation.ValidClassname;
import org.hibernate.validator.constraints.Length;

/**
 * @author Aristide Cittadino Model class for Algorithm of HyperIoT platform. This
 * class is used to map Algorithm with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class Algorithm extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity {

    /**
     * Name of algorithm
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String name;
    /**
     * Description of algorithm
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String description;
    /**
     * String, in JSON format, containing base configuration of algorithm (i.e. inputs and outputs)
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String baseConfig;
    /**
     * Name of Spark job file
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String algorithmFileName;

    /**
     * Source file path
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String algorithmFilePath;

    /**
     * Algorithm Type
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private AlgorithmType type;

    /**
     * Name of class containing Spark job main method
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private String mainClassname;

    /**
     * Get name of algorithm
     *
     * @return name of algorithm
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Size(max = 255)
    public String getName() {
        return name;
    }

    /**
     * Set name of algorithm
     *
     * @param name name of algorithm
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get description of algorithm
     *
     * @return description of algorithm
     */
    @NoMalitiusCode
    @Column(length = 3000)
    @Size(max = 3000)
    public String getDescription() {
        return description;
    }

    /**
     * Set description of algorithm
     *
     * @param description description of algorithm
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get base configuration of algorithm
     *
     * @return base configuration of algorithm
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Column(columnDefinition = "TEXT")
    public String getBaseConfig() {
        return baseConfig;
    }

    /**
     * Set base configuration of algorithm
     *
     * @param baseConfig base configuration of algorithm
     */
    public void setBaseConfig(String baseConfig) {
        this.baseConfig = baseConfig;
    }

    /**
     * Get name of Spark job jar file
     *
     * @return name of Spark job jar file
     */
    @NoMalitiusCode
    @Length(max = 500)
    public String getAlgorithmFileName() {
        return algorithmFileName;
    }

    /**
     * Set name of Spark job jar file
     *
     * @param jarName name of Spark job jar file
     */
    public void setAlgorithmFileName(String jarName) {
        this.algorithmFileName = jarName;
    }

    @NoMalitiusCode
    @Length(max = 1000)
    public String getAlgorithmFilePath() {
        return algorithmFilePath;
    }

    public void setAlgorithmFilePath(String jarPath) {
        this.algorithmFilePath = jarPath;
    }

    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public AlgorithmType getType() {
        return type;
    }

    public void setType(AlgorithmType type) {
        this.type = type;
    }

    /**
     * Return name of class containing Spark job main method
     *
     * @return name of class containing Spark job main method
     */
    @NoMalitiusCode
    @ValidClassname
    public String getMainClassname() {
        return mainClassname;
    }

    /**
     * Set name of class containing Spark job main method
     *
     * @param mainClassname name of class containing Spark job main method
     */
    public void setMainClassname(String mainClassname) {
        this.mainClassname = mainClassname;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Algorithm other = (Algorithm) obj;
        if (this.getId() > 0 && other.getId() > 0)
            return this.getId() == other.getId();
        if (name == null) {
            return other.name == null;
        } else return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Algorithm{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", baseConfig='" + baseConfig + '\'' +
                '}';
    }

}
