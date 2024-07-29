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

package it.acsoftware.hyperiot.rule.service.actions;

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldMultiplicity;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.rule.model.actions.EnrichmentRuleAction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

public class FourierTransformRuleAction extends EnrichmentRuleAction {
    private static final Logger log = LoggerFactory.getLogger(FourierTransformRuleAction.class.getName());
    private long inputFieldId;
    private long outputFieldId;
    private String outputFieldName;

    public enum MethodType {
        FAST,
        DISCRETE
    }

    public enum FftTransformType {
        FORWARD,
        INVERSE
    }

    public enum FftNormalization {
        STANDARD,
        UNITARY
    }

    private MethodType transformMethod = MethodType.FAST;
    private FftNormalization fftNormalization = FftNormalization.STANDARD;
    private FftTransformType fftTransformType = FftTransformType.FORWARD;

    public FourierTransformRuleAction() {
        super();
    }

    @Override
    public String droolsDefinition() {
        log.debug("In FourierTransformRuleAction.droolsDefinition");
        StringBuilder sb = new StringBuilder();
        sb.append("FourierTransformRuleAction fourierTransform = new FourierTransformRuleAction();\n        ");
        String method = "transform";
        if (transformMethod == MethodType.FAST) {
            method = "fastTransform";
            sb.append("fourierTransform.setFftNormalization(FourierTransformRuleAction.FftNormalization.")
                    .append(fftNormalization).append(");\n        ")
                    .append("fourierTransform.setTransformType(FourierTransformRuleAction.FftTransformType.")
                    .append(fftTransformType).append(");\n        ");
        }
        sb.append("fourierTransform.").append(method).append("(") // <-- use this for FFT
                .append(this.getDroolsPacketNameVariable())
                .append(", ")
                .append(inputFieldId)
                .append(", ")
                .append(outputFieldId)
                .append(", \"")
                .append(outputFieldName)
                .append("\")");
        log.debug("partial Drool generated: {}", sb);
        return sb.toString();
    }


    public long getInputFieldId() {
        return inputFieldId;
    }

    public void setInputFieldId(long fieldId) {
        this.inputFieldId = fieldId;
    }

    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String fieldName) {
        this.outputFieldName = fieldName;
    }

    public long getOutputFieldId() {
        return outputFieldId;
    }

    public void setOutputFieldId(long fieldId) {
        this.outputFieldId = fieldId;
    }

    public void setTransformMethod(MethodType type) {
        transformMethod = type;
    }

    public void setFftNormalization(FftNormalization normalization) {
        fftNormalization = normalization;
    }

    public void setTransformType(FftTransformType type) {
        fftTransformType = type;
    }

    public void fastTransform(HPacket packet, long inputFieldId, long outputFieldId, String outputFieldName) {
        HPacketField field = packet.getFields().stream().filter((pf) -> pf.getId() == inputFieldId).findFirst().orElse(null);
        if (field == null) {
            //log.debug( "Missing input field with id {}", inputFieldId);
            return;
        }

        // Apply Fast Fourier Transform
        ArrayList<Double> transformedValues = new ArrayList<>();
        try {
            double[] values;
            if (field.getValue() instanceof ArrayList<?>) {
                values = ((ArrayList<Double>) field.getValue()).stream().mapToDouble(d -> d).toArray();
            } else {
                values = (double[]) field.getValue();
            }
            double[] inputValues = values;
            FastFourierTransformer transformer = new FastFourierTransformer(fftNormalization == FftNormalization.STANDARD ? DftNormalization.STANDARD : DftNormalization.UNITARY);
            Complex[] complx = transformer.transform(inputValues, fftTransformType == FftTransformType.FORWARD ? TransformType.FORWARD : TransformType.INVERSE);
            for (int i = 0; i < complx.length; i++) {
                double rr = (complx[i].getReal());
                double ri = (complx[i].getImaginary());
                transformedValues.add(Math.sqrt((rr * rr) + (ri * ri)));
            }
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
        }

        HPacketField outputField = packet
                .getFields().stream()
                .filter(f -> f.getId() == outputFieldId)
                .findFirst().orElse(null);

        if (outputField == null) {
            //log.debug( String.format("Creating output field with id %s and name %s", outputFieldId, outputFieldName));
            try {
                outputField = new HPacketField();
                outputField.setId(outputFieldId);
                outputField.setName(outputFieldName);
                outputField.setType(HPacketFieldType.OBJECT);
                outputField.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
                outputField.setCategoryIds(new long[0]);
                outputField.setTagIds(new long[0]);
                outputField.setInnerFields(new HashSet<>());
                outputField.setPacket(packet);
                packet.getFields().add(outputField);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
                e.printStackTrace();
                throw e;
            }

        }

        // Store transformed value to output field
        try {
            outputField.setValue(transformedValues);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void transform(HPacket packet, long inputFieldId, long outputFieldId, String outputFieldName) {
        HPacketField field = packet.getFields().stream().filter((pf) -> pf.getId() == inputFieldId).findFirst().orElse(null);
        if (field == null) {
            //log.debug( "Missing input field with id {}", inputFieldId);
            return;
        }

        ArrayList<Double> transformedValues = new ArrayList<>();
        // Apply Discrete Fourier Transform (real input)
        try {
            double[] values;
            if (field.getValue() instanceof ArrayList<?>) {
                values = ((ArrayList<Double>) field.getValue()).stream().mapToDouble(d -> d).toArray();
            } else {
                values = (double[]) field.getValue();
            }
            double[] inputValues = values;
            double[] outreal = new double[inputValues.length];
            double[] outimag = new double[inputValues.length];
            dftR(inputValues, outreal, outimag);
            for (int i = 0; i < outreal.length; i++) {
                double rr = outreal[i];
                double ri = outimag[i];
                transformedValues.add(Math.sqrt((rr * rr) + (ri * ri)));
            }
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
        }

        HPacketField outputField = packet
                .getFields().stream()
                .filter(f -> f.getId() == outputFieldId)
                .findFirst().orElse(null);

        if (outputField == null) {
            // Auto-create output field if it does not exists
            //log.debug( String.format("Creating output field with id %s and name %s", outputFieldId, outputFieldName));
            try {
                outputField = new HPacketField();
                outputField.setId(outputFieldId);
                outputField.setName(outputFieldName);
                outputField.setType(HPacketFieldType.OBJECT);
                outputField.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
                outputField.setCategoryIds(new long[0]);
                outputField.setTagIds(new long[0]);
                outputField.setInnerFields(new HashSet<>());
                outputField.setPacket(packet);
                packet.getFields().add(outputField);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
                e.printStackTrace();
                throw e;
            }
        }

        // Store transformed value to output field
        try {
            outputField.setValue(transformedValues);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector.
     * All the array arguments must be non-null and have the same length.
     */
    public void dft(double[] inreal, double[] inimag, double[] outreal, double[] outimag) {
        int n = inreal.length;
        for (int k = 0; k < n; k++) {  // For each output element
            double sumreal = 0;
            double sumimag = 0;
            for (int t = 0; t < n; t++) {  // For each input element
                double angle = 2 * Math.PI * t * k / n;
                sumreal += inreal[t] * Math.cos(angle) + inimag[t] * Math.sin(angle);
                sumimag += -inreal[t] * Math.sin(angle) + inimag[t] * Math.cos(angle);
            }
            outreal[k] = sumreal;
            outimag[k] = sumimag;
        }
    }

    /*
     * Computes the discrete Fourier transform (DFT) of the given real vector.
     * All the array arguments must be non-null and have the same length.
     */
    public void dftR(double[] inreal, double[] outreal, double[] outimag) {
        double[] inimag = new double[inreal.length];
        dft(inreal, inimag, outreal, outimag);
    }
}
