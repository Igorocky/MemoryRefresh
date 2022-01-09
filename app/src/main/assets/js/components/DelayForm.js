"use strict";

const ALL_DELAY_UNITS = ['Seconds', 'Minutes', 'Hours', 'Days', 'Months',]
const ALL_DELAY_UNITS_SHORT = ['s', 'm', 'h', 'd', 'M',]
const DELAY_REGEX = /^(\d+)(s|m|h|d|M)?$/

const DelayForm = ({initialDelay, delay, delayOnChange, onSubmit, onKeyDown, result,
                       delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress}) => {

    function parseDelayStr(text) {
        const delayParseRes = DELAY_REGEX.exec(text)
        return {delayAmount: delayParseRes?.[1], delayUnit: delayParseRes?.[2]}
    }

    const {delayAmount: initialDelayAmount, delayUnit: initialDelayUnit} = parseDelayStr(initialDelay)
    const {delayAmount, delayUnit:delayUnitOrig} = parseDelayStr(delay)
    const delayUnit = delayUnitOrig??initialDelayUnit

    function renderSubmitButton() {
        if (updateDelayRequestIsInProgress) {
            return RE.span({}, RE.CircularProgress({size:24, style: {marginLeft: '5px'}}))
        } else {
            return iconButton({
                iconName: 'done',
                onClick: onSubmit,
                iconStyle: {color: 'blue'}
            })
        }
    }

    return RE.Container.row.left.center({},{style:{marginRight:'10px'}},
        RE.TextField({
            ref: delayTextFieldRef,
            id: delayTextFieldId,
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value: delayAmount??delay,
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            inputProps: {size:3, tabIndex:delayTextFieldTabIndex},
            color: hasValue(delayAmount)?'primary':'secondary',
            onChange: event => {
                const newText = event.nativeEvent.target.value.trim()
                const {delayAmount: newDelayAmount, delayUnit: newDelayUnit} = parseDelayStr(newText)
                if (newDelayAmount) {
                    delayOnChange(newDelayAmount + (newDelayUnit??delayUnit))
                } else {
                    delayOnChange(newText)
                }
            },
            onKeyUp: event => (event.keyCode === ENTER_KEY_CODE) ? onSubmit() : null,
            onKeyDown: onKeyDown,
        }),
        RE.FormControl({variant:"outlined"},
            RE.InputLabel({id:'unit-select'}, 'Unit'),
            RE.Select(
                {
                    value:delayUnit,
                    disabled:hasNoValue(delayAmount),
                    variant: 'outlined',
                    label: 'Unit',
                    labelId: 'unit-select',
                    onChange: event => {
                        const newUnit = event.target.value
                        delayOnChange(delayAmount + newUnit)
                    }
                },
                ALL_DELAY_UNITS.map((unitDispName,idx) => RE.MenuItem({key:idx, value:ALL_DELAY_UNITS_SHORT[idx]}, ALL_DELAY_UNITS[idx]))
            )
        ),
        RE.span({style:{fontSize: '15px', fontFamily:'monospace', fontWeight:'bold'}}, result),
        renderSubmitButton()
    )
}
