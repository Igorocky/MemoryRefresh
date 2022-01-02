'use strict';

const TagSelector = ({allTags, selectedTags, onTagSelected, onTagRemoved, label, color, minimized = false, selectedTagsBgColor}) => {

    const [filterText, setFilterText] = useState('')
    let selectedTagIds = selectedTags.map(t=>t.id)

    function renderSelectedTags() {
        return RE.Fragment({},
            selectedTags.map(tag => RE.Chip({
                style: {marginRight:'10px', marginBottom:'5px'},
                key:tag.id,
                variant:'outlined',
                size:'small',
                onDelete: () => onTagRemoved(tag),
                label: tag.name,
                color:color??'default',
            }))
        )
    }

    function renderTagFilter() {
        if (allTags?.length) {
            return RE.TextField(
                {
                    autoCorrect:'off', autoCapitalize:'none', spellCheck:'false',
                    variant: 'outlined',
                    style: {width: 200},
                    size: 'small',
                    label,
                    onChange: event => setFilterText(event.nativeEvent.target.value.trim().toLowerCase()),
                    value: filterText
                }
            )
        }
    }

    function renderFilteredTags() {
        if (allTags) {
            let notSelectedTags = allTags.filter(t => !selectedTagIds.includes(t.id))
            let filteredTags = filterText.length == 0 ? notSelectedTags : notSelectedTags.filter(tag => tag.name.toLowerCase().indexOf(filterText) >= 0)
            if (filteredTags.length == 0) {
                if (filterText.length) {
                    return 'No tags match the search criteria'
                } else {
                    return 'No tags remain'
                }
            } else {
                return RE.Container.row.left.center(
                    {style:{maxHeight:'250px', overflow:'auto'}},
                    {style:{margin:'3px'}},
                    filteredTags.map(tag => RE.Chip({
                        style: {marginRight:'3px'},
                        variant:'outlined',
                        size:'small',
                        onClick: () => {
                            setFilterText('')
                            onTagSelected(tag)
                        },
                        label: tag.name,
                    }))
                )
            }
        } else {
            return 'Loading tags...'
        }
    }

    return RE.Container.col.top.left({},{style:{margin:'3px'}},
        RE.div({style:{backgroundColor:selectedTagsBgColor}}, renderSelectedTags()),
        RE.IfNot(minimized, () => renderTagFilter()),
        RE.IfNot(minimized, () => renderFilteredTags())
    )
}