package f95gm.build

/** Stable facade for the parsed OpenAPI contract model. */
object OpenApiContractModel {
    fun parse(contractJson: String): OpenApiDocumentModel = OpenApiContractParser.parse(contractJson)
    fun parse(document: Map<*, *>): OpenApiDocumentModel = OpenApiContractParser.parse(document)
    fun operations(contractJson: String): List<OpenApiOperationModel> = OpenApiContractParser.operations(contractJson)
}
