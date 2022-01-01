'use strict';

const IntRangeSelector = ({selectedMin = null, selectedMax = null, onMinSelected, onMaxSelected, parameterName, minimized = false}) => {

    function getSelectedMin() {
        return hasValue(selectedMin) ? selectedMin : ''
    }

    function getSelectedMax() {
        return hasValue(selectedMax) ? selectedMax : ''
    }

    function minSelected(min) {
        onMinSelected(min.length ? min : null)
    }

    function maxSelected(max) {
        onMaxSelected(max.length ? max : null)
    }

    if (minimized) {
        return RE.span(
            {style: {marginLeft: '5px', color:'blue'}},
            `${(hasValue(selectedMin) ? selectedMin : 0) + ' \u2264 '}${parameterName}${hasValue(selectedMax) ? ' \u2264 ' + selectedMax : ''}`
        )
    } else {
        return RE.Container.row.left.center({},{style:{margin:'5px'}},
            RE.TextField({
                autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
                value: getSelectedMin(),
                label: 'Min',
                variant: 'outlined',
                multiline: false,
                maxRows: 1,
                size: 'small',
                inputProps: {size:4},
                onChange: event => {
                    const newText = event.nativeEvent.target.value
                    if (newText !== selectedMin && (/^\d*$/g).test(newText)) {
                        minSelected(newText)
                    }
                },
            }),
            RE.TextField({
                autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
                value: getSelectedMax(),
                label: 'Max',
                variant: 'outlined',
                multiline: false,
                maxRows: 1,
                size: 'small',
                inputProps: {size:4},
                onChange: event => {
                    const newText = event.nativeEvent.target.value
                    console.log('typeof newText', typeof newText)
                    console.log('newText', newText)
                    if (newText !== selectedMax && (/^\d*$/g).test(newText)) {
                        maxSelected(newText)
                    }
                },
            }),
        )
    }
}