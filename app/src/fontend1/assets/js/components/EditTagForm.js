"use strict";

const EditTagForm = ({name,tagNameTextFieldLabel = '',onNameChange,onSave,onCancel}) => {

    function renderButtons() {
        return RE.Fragment({},
            RE.IconButton({onClick: onCancel}, RE.Icon({style:{color:'blue'}}, 'close')),
            RE.IconButton({onClick: onSave}, RE.Icon({style:{color:'blue'}}, 'save')),
        )
    }

    return RE.Container.row.left.center({},{},
        textField({
            value:name,
            variant:'outlined',
            autoFocus:true,
            size:'small',
            label:tagNameTextFieldLabel,
            onChange: event => {
                onNameChange(event.nativeEvent.target.value)
            },
            onKeyDown: event =>
                event.nativeEvent.keyCode == 13 ? onSave()
                    : event.nativeEvent.keyCode == 27 ? onCancel()
                        : null,
        }),
        renderButtons()
    )
}
