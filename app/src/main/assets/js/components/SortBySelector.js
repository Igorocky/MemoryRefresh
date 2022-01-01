'use strict';

const SortBySelector = ({possibleParams, selectedParam, onParamSelected, possibleDirs, selectedDir, onDirSelected, minimized = false}) => {

    if (minimized) {
        return RE.span(
            {style: {marginLeft: '5px', marginRight: '5px', color:'blue'}},
            possibleParams[selectedParam].displayName + ' ' + possibleDirs[selectedDir].displayName
        )
    } else {
        return RE.Container.row.left.center({},{style:{margin:'5px'}},
            RE.FormControl({variant:"outlined"},
                RE.InputLabel({id:'param-select'}, 'Sort by'),
                RE.Select(
                    {
                        value:selectedParam,
                        variant: 'outlined',
                        label: 'Sort by',
                        labelId: 'param-select',
                        onChange: event => {
                            const newParam = event.target.value
                            onParamSelected(newParam)
                        }
                    },
                    Object.keys(possibleParams).map(param => RE.MenuItem({key:param, value:param}, possibleParams[param].displayName))
                )
            ),
            RE.FormControl({variant:"outlined"},
                RE.InputLabel({id:'dir-select'}, 'Direction'),
                RE.Select(
                    {
                        value:selectedDir,
                        variant: 'outlined',
                        label: 'Direction',
                        labelId: 'dir-select',
                        style:{marginRight:'5px'},
                        onChange: event => {
                            const newDir = event.target.value
                            onDirSelected(newDir)
                        }
                    },
                    Object.keys(possibleDirs).map(dir => RE.MenuItem({key:dir, value:dir}, possibleDirs[dir].displayName))
                )
            ),
        )
    }
}