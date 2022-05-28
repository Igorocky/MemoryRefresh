'use strict';

function directionToStr(dir) {
    if (dir === 'NATIVE_FOREIGN') {
        return 'native\u{02192}foreign'
    } else if (dir === 'FOREIGN_NATIVE') {
        return 'foreign\u{02192}native'
    }
}

const DirectionSelector = ({selectedDirection, onDirectionSelected, minimized = false}) => {
    if (minimized) {
        return RE.span({style: {marginLeft: '5px', color:'blue'}},`${selectedDirection}`)
    } else {
        return RE.Container.row.left.center({},{style:{margin:'5px'}},
            RE.FormControl({variant:"outlined"},
                RE.InputLabel({id:'direction-select'}, 'Direction'),
                RE.Select(
                    {
                        value:selectedDirection,
                        variant: 'outlined',
                        label: 'Direction',
                        labelId: 'direction-select',
                        onChange: event => {
                            const newDirection = event.target.value
                            onDirectionSelected(newDirection)
                        }
                    },
                    ['NATIVE_FOREIGN', 'FOREIGN_NATIVE'].map((dir, idx) =>
                        RE.MenuItem({key:idx, value:dir}, directionToStr(dir))
                    )
                )
            ),
        )
    }
}