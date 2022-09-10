"use strict";

const KeyPad = ({componentKey, keys, size, variant, buttonWidth, onKeyUp, orientation}) => {
    function getKeyContent(key) {
        if (hasValue(key.symbol)) {
            return key.symbol
        } else if (hasValue(key.icon)) {
            return key.icon
        } else if (hasValue(key.iconName)) {
            return RE.Icon({}, key.iconName)
        }
    }

    return RE.Container.col.top.left({key:componentKey}, {style: {marginBottom:"1px"}},
        keys.map(row => RE.ButtonGroup({key:row.map(({key}) => key).join(','), variant:variant?variant:"contained", size:size?size:"large", onKeyUp: ({nativeEvent}) => onKeyUp?.(nativeEvent), orientation},
            row.map(key => RE.Button({
                    key:key.key,
                    style:{width:buttonWidth?buttonWidth:"1em", ...(key.style?key.style:{})},
                    disabled: key.disabled,
                    onClick: () => key.onClick?key.onClick():null,
                },
                getKeyContent(key)
            ))
        ))
    )
}