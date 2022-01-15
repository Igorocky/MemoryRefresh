"use strict";

const DelayForm = ({actualDelay, initialDelay, delay, delayOnChange, result, onSubmit, onKeyDown,
                       delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress}) => {

    function renderSubmitButton() {
        if (updateDelayRequestIsInProgress) {
            return RE.span({}, RE.CircularProgress({size:24, style: {marginLeft: '5px'}}))
        } else {
            return iconButton({
                iconName: 'done',
                onClick: onSubmit,
                iconStyle: {color: hasValue(result) ? 'blue' : 'grey'},
                disabled: hasNoValue(result),
            })
        }
    }

    return RE.Container.row.left.center({},{style:{marginRight:'10px'}},
        RE.span({style:{fontSize: '15px', color:'gray'}}, `[${actualDelay}]`),
        RE.span({style:{fontSize: '15px', color:'gray'}}, initialDelay),
        RE.span({style:{fontSize: '15px', color:'gray'}}, '\u{02192}'),
        textField({
            ref: delayTextFieldRef,
            id: delayTextFieldId,
            value: delay,
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            inputProps: {size:3, tabIndex:delayTextFieldTabIndex},
            color: hasValue(result)?'primary':'secondary',
            size:'small',
            onChange: event => delayOnChange(event.nativeEvent.target.value.trim()),
            onKeyUp: event => (event.keyCode === ENTER_KEY_CODE) ? onSubmit() : null,
            onKeyDown: onKeyDown,
        }),
        RE.span({style:{fontSize: '15px', fontFamily:'monospace', fontWeight:'bold'}}, result),
        renderSubmitButton()
    )
}
