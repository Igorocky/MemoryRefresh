'use strict';

const TagSelector = ({allTags, selectedTags, onTagSelected, onTagRemoved, label, color, minimized = false, selectedTagsBgColor, autoFocus}) => {

    const [filterText, setFilterText] = useState('')
    let selectedTagIds = selectedTags.map(t=>t.id)
    const filterTextFieldRef = useRef(null)

    function renderSelectedTags() {
        return RE.Fragment({},
            selectedTags.map(tag => RE.Chip({
                style: {marginRight:'10px', marginBottom:'5px'},
                key:tag.id,
                variant:'outlined',
                size:'small',
                onDelete: () => {
                    onTagRemoved(tag)
                    focusFilterText()
                },
                label: tag.name,
                color:color??'default',
            }))
        )
    }

    function getFilteredTags() {
        if (allTags) {
            let notSelectedTags = allTags.filter(t => !selectedTagIds.includes(t.id))
            return filterText.length == 0 ? notSelectedTags : notSelectedTags.filter(tag => tag.name.toLowerCase().indexOf(filterText) >= 0)
        } else {
            return []
        }
    }

    const filteredTags = getFilteredTags()
    useEffect(() => {
        if (IS_IN_WEBVIEW && filteredTags.length === 1 && filterText.length !== 0) {
            selectTag(filteredTags[0])
        }
    }, [filteredTags.length])

    function renderTagFilter() {
        if (allTags?.length) {
            return textField(
                {
                    ref: filterTextFieldRef,
                    variant: 'outlined',
                    style: {width: 200},
                    size: 'small',
                    autoFocus,
                    label,
                    onChange: event => setFilterText(event.nativeEvent.target.value.trim().toLowerCase()),
                    value: filterText,
                    onKeyDown: event => {
                        if (event.keyCode === ENTER_KEY_CODE && filteredTags.length === 1) {
                            selectTag(filteredTags[0])
                        }
                    },
                }
            )
        }
    }

    function focusFilterText() {
        if (filterTextFieldRef.current) {
            filterTextFieldRef.current.getElementsByTagName('input')[0].focus()
        }
    }

    function selectTag(tag) {
        setFilterText('')
        focusFilterText()
        onTagSelected(tag)
    }

    function renderFilteredTags() {
        if (allTags) {
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
                            selectTag(tag)
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