'use strict';

const LESS_EQ_CHAR = '\u{2264}'

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
        return null
    } else {
        return RE.Container.row.left.center({},{style:{margin:'5px'}},
            textField({
                value: getSelectedMin(),
                label: 'Min',
                variant: 'outlined',
                autoFocus:true,
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
            textField({
                value: getSelectedMax(),
                label: 'Max',
                variant: 'outlined',
                multiline: false,
                maxRows: 1,
                size: 'small',
                inputProps: {size:4},
                onChange: event => {
                    const newText = event.nativeEvent.target.value
                    if (newText !== selectedMax && (/^\d*$/g).test(newText)) {
                        maxSelected(newText)
                    }
                },
            }),
        )
    }
}