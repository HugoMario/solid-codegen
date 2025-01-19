package org.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;

public class CodegenSchema {

    private CodegenModel codegenModel;
    private Schema schema;

    public CodegenSchema(CodegenModel codegenModel, Schema schema) {
        this.codegenModel = codegenModel;
        this.schema = schema;
    }

    public CodegenModel getCodegenModel() {
        return codegenModel;
    }

    public Schema getSchema() {
        return schema;
    }
}
