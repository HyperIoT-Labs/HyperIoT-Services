package it.acsoftware.hyperiot.algorithm.api;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;

import java.io.File;

/**
 * 
 * @author Aristide Cittadino Interface component for Algorithm Repository.
 *         It is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface AlgorithmRepository extends HyperIoTBaseRepository<Algorithm> {

    Algorithm updateAlgorithmFile(Algorithm algorithm, String mainClassname, File algorithmFile);

}
