package org.openapi.codegen.service;

import org.openapi.codegen.ClientOptInput;
import org.openapi.codegen.DefaultGenerator;
import org.openapi.codegen.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class GeneratorService {
    protected final Logger LOGGER = LoggerFactory.getLogger(GeneratorService.class);

    private GenerationRequest generationRequest;
    private ClientOptInput optsV3;
    private io.swagger.codegen.ClientOptInput optsV2;

    public GeneratorService generationRequest(GenerationRequest generationRequest) {
        this.generationRequest = generationRequest;
        if (GenerationRequest.CodegenVersion.V2.equals(generationRequest.getCodegenVersion())) {
            final io.swagger.codegen.ClientOptInput clientOptInputV2;
            try {
                clientOptInputV2 = GeneratorUtil.getClientOptInputV2(generationRequest);
            } catch (Exception e) {
                String msg = "Error processing input options: " + e.getMessage();
                LOGGER.error(msg, e);
                throw new BadRequestException(msg);
            }
            optsV2 = clientOptInputV2;
        } else {
            final ClientOptInput clientOptInput;
            try {
                clientOptInput = GeneratorUtil.getClientOptInput(generationRequest);
            } catch (Exception e) {
                String msg = "Error processing input options: " + e.getMessage();
                LOGGER.error(msg, e);
                throw new BadRequestException(msg);
            }
            optsV3 = clientOptInput;
        }

        return this;
    }

    public List<File> generate() {
        if (optsV3 != null) {
            return new DefaultGenerator().opts(optsV3).generate();
        } else if (optsV2 != null) {
            return new io.swagger.codegen.DefaultGenerator().opts(optsV2).generate();
        }
        throw new RuntimeException("missing opts input");
    }

    public Map<String, Object> generateBundle() {
        if (optsV3 != null) {
            return new DefaultGenerator().opts(optsV3).generateBundle();
        }
        throw new RuntimeException("missing opts input");
    }

    public String renderTemplate(RenderRequest renderRequest) {
        return new DefaultGenerator().renderTemplate(renderRequest.getTemplate(), renderRequest.getContext());
    }

}