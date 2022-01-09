"use strict";

const EditTranslateCardForm = ({
                                    allTags, allTagsMap,
                                    paused,pausedOnChange,pausedBgColor,
                                    textToTranslate,textToTranslateOnChange,textToTranslateBgColor,textToTranslateId,
                                    translation,translationOnChange,translationBgColor,
                                    delay,delayOnChange,delayBgColor,
                                    tagIds,tagIdsOnChange,tagIdsBgColor,
                                    activatesIn,createdAt,
                                    onSave, saveDisabled,
                                    onCancel, cancelDisabled,
                                    onDelete,
                                }) => {

    function renderSaveButton() {
        return iconButton({
            iconName: 'save',
            disabled: saveDisabled,
            iconStyle:{color:saveDisabled?'lightgrey':'blue'},
            onClick: onSave
        })
    }

    function renderCancelButton() {
        if (onCancel) {
            return iconButton({
                iconName: 'close',
                disabled: cancelDisabled,
                iconStyle:{color:'black'},
                onClick: onCancel
            })
        }
    }

    function renderDeleteButton() {
        if (onDelete) {
            return iconButton({
                iconName: 'delete',
                iconStyle:{color:'red'},
                onClick: onDelete
            })
        }
    }

    function renderButtons() {
        return RE.Container.row.left.center({}, {},
            renderDeleteButton(),
            renderCancelButton(),
            renderSaveButton()
        )
    }

    const margin = '30px'

    return RE.Container.col.top.left({}, {},
        renderButtons(),
        RE.If(hasValue(textToTranslate), () => RE.TextField({
            id: textToTranslateId,
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value: textToTranslate,
            label: 'Native text',
            variant: 'outlined',
            autoFocus: false,
            multiline: true,
            maxRows: 10,
            size: 'small',
            style: {backgroundColor:textToTranslateBgColor, marginTop:margin},
            inputProps: {cols:27},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != textToTranslate) {
                    textToTranslateOnChange(newText)
                }
            },
            onKeyUp: event => {
                if (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) {
                    if (!saveDisabled) {
                        onSave?.()
                    }
                } else if (event.nativeEvent.keyCode == ESCAPE_KEY_CODE) {
                    onCancel?.()
                }
            },
        })),
        RE.If(hasValue(translation), () => RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value: translation,
            label: 'Foreign text',
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            style: {backgroundColor:translationBgColor, marginTop:margin},
            inputProps: {cols:27},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != translation) {
                    translationOnChange(newText)
                }
            },
            onKeyUp: event => {
                if (event.ctrlKey && event.keyCode === ENTER_KEY_CODE) {
                    if (!saveDisabled) {
                        onSave?.()
                    }
                } else if (event.nativeEvent.keyCode == ESCAPE_KEY_CODE) {
                    onCancel?.()
                }
            },
        })),
        RE.If(hasValue(paused), () => RE.FormGroup({style:{backgroundColor:pausedBgColor, marginTop:'20px'}},
            RE.FormControlLabel({
                control: RE.Checkbox({checked: paused, onChange: event => pausedOnChange(event.target.checked)}),
                label:"Paused"
            })
        )),
        RE.If(hasValue(tagIds), () => RE.Paper({style: {marginTop:margin}},re(TagSelector,{
            allTags,
            selectedTags: tagIds.map(tid => allTagsMap[tid]),
            onTagRemoved:tag=>{
                tagIdsOnChange(tagIds.filter(tid=>tid!=tag.id))
            },
            onTagSelected:tag=>{
                tagIdsOnChange([...tagIds,tag.id])
            },
            label: 'Tags',
            color:'primary',
            selectedTagsBgColor:tagIdsBgColor
        }))),
        RE.If(hasValue(delay), () => RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value: delay,
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            style: {backgroundColor:delayBgColor, marginTop:margin},
            inputProps: {size:8},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != delay) {
                    delayOnChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == ESCAPE_KEY_CODE ? onCancel?.() : null,
        })),
        RE.If(hasValue(activatesIn), () => RE.span({style:{marginTop:margin}}, activatesIn === '-' ? 'This card is accessible' : `Becomes accessible in: ${activatesIn}`)),
        RE.If(hasValue(createdAt), () => RE.div({style: {marginTop:margin}}, `Created: ${createdAt}`)),
        renderButtons(),
    )
}
