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

import java.util.Objects;

public class AlgorithmIOField {

    /**
     * Do not se this ID as a db key constraint! It is not a mapped entity, however we need ID to update output field
     */
    private long id;
    private String name;
    private String description;
    private AlgorithmIOFieldType fieldType;
    private AlgorithmIOFieldMultiplicity multiplicity;
    /**
     * Field type, i.e. INPUT or OUTPUT
     */
    private AlgorithmFieldType type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AlgorithmIOFieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(AlgorithmIOFieldType fieldType) {
        this.fieldType = fieldType;
    }

    public AlgorithmIOFieldMultiplicity getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(AlgorithmIOFieldMultiplicity multiplicity) {
        this.multiplicity = multiplicity;
    }

    public AlgorithmFieldType getType() {
        return type;
    }

    public void setType(AlgorithmFieldType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmIOField that = (AlgorithmIOField) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "AlgorithmIOField{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", ioFieldType=" + fieldType +
                ", multiplicity=" + multiplicity +
                ", type=" + type +
                '}';
    }

}
