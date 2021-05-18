package com.joelspecht.pi4ign.gateway

import com.inductiveautomation.ignition.gateway.localdb.persistence.FormMeta
import com.inductiveautomation.ignition.gateway.model.GatewayContext
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode
import com.inductiveautomation.ignition.gateway.web.components.editors.IEditorSource
import com.inductiveautomation.ignition.gateway.web.components.editors.ReferenceEditor
import com.inductiveautomation.ignition.gateway.web.models.IRecordFieldComponent
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel
import com.pi4j.Pi4J
import com.pi4j.context.Context
import org.apache.wicket.Component
import org.apache.wicket.markup.html.form.DropDownChoice
import org.apache.wicket.markup.html.form.IChoiceRenderer
import org.apache.wicket.protocol.http.WebApplication
import simpleorm.dataset.SFieldMeta
import simpleorm.dataset.SRecordInstance
import java.util.function.Function

class DropDownEditorSource(@Transient private val choiceFactory: Function<Context, List<Pair<String, String>>>) :
    IEditorSource {

    override fun newEditorComponent(
        id: String?,
        editMode: RecordEditMode?,
        record: SRecordInstance?,
        formMeta: FormMeta?
    ): Component {
        val dropdown = PersistentFieldDropdownChoice("editor", formMeta!!.field)
        formMeta.installValidators(dropdown)
        dropdown.label = LenientResourceModel(formMeta.fieldNameKey)

        val servletContext = WebApplication.get().servletContext
        val gatewayContext = servletContext.getAttribute(GatewayContext.SERVLET_CONTEXT_KEY) as GatewayContext
        val hook: GatewayHook = GatewayHook.get(gatewayContext)
        val piContext = hook.runWithModuleClassLoader(Pi4J::newAutoContext)
        val choices: List<Pair<String, String>>
        try {
            choices = choiceFactory.apply(piContext)
        } finally {
            piContext.shutdown()
        }
        val renderer = Renderer(choices.toMap())
        dropdown.choiceRenderer = renderer
        dropdown.choices = choices.map { it.first }

        return ReferenceEditor(id, formMeta, editMode, record, false, dropdown)
    }

    private inner class Renderer(private val choices: Map<String, String>) : IChoiceRenderer<String> {

        override fun getDisplayValue(`object`: String?): Any {
            return if (`object` == null) {
                ""
            } else {
                choices[`object`] ?: ""
            }
        }

        override fun getIdValue(`object`: String?, index: Int): String {
            return `object` ?: index.toString()
        }

    }

    private inner class PersistentFieldDropdownChoice(id: String, @Transient private val fieldMeta: SFieldMeta) :
        DropDownChoice<String>(id), IRecordFieldComponent {

        override fun getFieldMeta(): SFieldMeta {
            return fieldMeta
        }

    }

}
