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

package it.acsoftware.hyperiot.stormmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class HyperIoTTopologyError {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
    @JsonIgnore
    private String stackTrace;

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public static HyperIoTTopologyError.HyperIoTTopologyErrorBuilder builder() {
        return new HyperIoTTopologyError.HyperIoTTopologyErrorBuilder();
    }

    public static class HyperIoTTopologyErrorBuilder {

        private HyperIoTTopologyError hyperIoTTopologyError;

        public HyperIoTTopologyErrorBuilder() {
            hyperIoTTopologyError = new HyperIoTTopologyError();
        }

        public HyperIoTTopologyErrorBuilder errorType(String errorType) {
            hyperIoTTopologyError.setErrorType(errorType);
            return this;
        }

        public HyperIoTTopologyErrorBuilder errorMessage(String errorMessage) {
            hyperIoTTopologyError.setErrorMessage(errorMessage);
            return this;
        }

        public HyperIoTTopologyErrorBuilder stackTrace(String stackTrace) {
            hyperIoTTopologyError.setStackTrace(stackTrace);
            return this;
        }

        public HyperIoTTopologyError build() {
            return hyperIoTTopologyError;
        }

    }

}
