"use strict";

const TextParamView = ({paramName,paramValue,editable = true,onSave,validator,isPassword = false}) => {
    const [newParamValue, setNewParamValue] = useState(null)
    const [isInvalidValue, setIsInvalidValue] = useState(false)
    const [isShowingPassword, setIsShowingPassword] = useState(false)
    const [isFocused, setIsFocused] = useState(false)

    async function save() {
        if (!(validator?.(newParamValue)??true)) {
            setIsInvalidValue(true)
        } else {
            await onSave(newParamValue)
            setNewParamValue(null)
            setIsInvalidValue(false)
            setIsShowingPassword(false)
            setIsFocused(false)
        }
    }

    function cancel() {
        setNewParamValue(null)
        setIsInvalidValue(false)
        setIsShowingPassword(false)
        setIsFocused(false)
    }

    function isEditMode() {
        return newParamValue != null
    }

    function beginEdit() {
        setNewParamValue(paramValue)
    }

    function getValue() {
        return (!isEditMode() && isPassword && !isShowingPassword) ? '********' : (newParamValue??paramValue)
    }

    function getTextFieldType() {
        return isEditMode() && isPassword && !isShowingPassword ? 'password' : 'text'
    }

    function renderShowPasswordButton() {
        return RE.IconButton(
            {onClick: () => setIsShowingPassword(prevValue => !prevValue)},
            RE.Icon(
                {style: {color: 'black'}},
                isShowingPassword ? 'visibility_off' : 'visibility'
            )
        )
    }

    function renderButtons() {
        if (isFocused) {
            if (isEditMode()) {
                return RE.Fragment({},
                    RE.IconButton({onClick:cancel}, RE.Icon({style:{color:'black'}}, 'highlight_off')),
                    RE.If(isPassword, () => renderShowPasswordButton()),
                    RE.IconButton({onClick: save}, RE.Icon({style:{color:'black'}}, 'save'))
                )
            } else if (editable) {
                return RE.Fragment({},
                    RE.IconButton({onClick: beginEdit}, RE.Icon({style:{color:'black'}}, 'edit')),
                    RE.If(isPassword, () => renderShowPasswordButton()),
                )
            }
        }
    }

    return RE.Container.row.left.center({},{},
        RE.TextField({
            autoCorrect:'off', autoCapitalize:'none', spellCheck:'false',
            label:paramName,
            value:getValue(),
            type:getTextFieldType(),
            error:isInvalidValue,
            variant:isEditMode()?'outlined':'standard',
            autoFocus:true,
            size:'small',
            disabled: newParamValue == null,
            onChange: event => setNewParamValue(event.nativeEvent.target.value),
            onKeyUp: event =>
                event.nativeEvent.keyCode == 13 ? save()
                    : event.nativeEvent.keyCode == 27 ? cancel()
                        : null,
            onClick: () => setIsFocused(prev => isEditMode() || !prev)
        }),
        renderButtons(),
    )
}
