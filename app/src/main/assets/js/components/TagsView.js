"use strict";

const TagsView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, confirmAction, showError} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [errorLoadingTags, setErrorLoadingTags] = useState(null)

    const [focusedTagId, setFocusedTagId] = useState(null)

    const [createNewTagMode, setCreateNewTagMode] = useState(false)
    const [editMode, setEditMode] = useState(false)

    useEffect(async () => {
        if (hasNoValue(allTags)) {
            const res = await be.readAllTags()
            if (res.err) {
                setErrorLoadingTags(res.err)
                showError(res.err)
            } else {
                setAllTags(res.data)
            }
        }
    }, [allTags])

    function reloadAllTags() {
        setAllTags(null)
    }

    function renderListOfTags() {
        if (allTags.length == 0) {
            return 'There are no tags.'
        } else {
            return re(ListOfObjectsCmp,{
                objects: allTags,
                beginIdx: 0,
                endIdx: allTags.length-1,
                onObjectClicked: tagId => editMode ? null : setFocusedTagId(prev => prev !== tagId ? tagId : null),
                renderObject: tag => RE.Paper({}, renderTag(tag))
            })
        }
    }

    async function deleteTag({tag}) {
        if (await confirmAction({text: `Delete tag '${tag.name}'?`, okBtnColor: 'secondary'})) {
            const res = await be.deleteTag({tagId:tag.id})
            if (res.err) {
                showError(res.err)
            } else {
                reloadAllTags()
            }
        }
    }

    function renderTag(tag) {
        if (focusedTagId === tag.id) {
            if (editMode) {
                return re(EditTagCmp, {
                    tagId: tag.id,
                    tagName: tag.name,
                    onSaved: () => {
                        setEditMode(false)
                        reloadAllTags()
                    },
                    onCanceled: () => setEditMode(false)
                })
            } else {
                return RE.Container.row.left.center({},{},
                    iconButton({iconName: 'delete', onClick: () => deleteTag({tag})}),
                    iconButton({iconName: 'edit', onClick: () => setEditMode(true)}),
                    RE.span({style: {marginLeft:'5px', marginRight:'5px'}}, tag.name)
                )
            }
        } else {
            return RE.span({style: {marginLeft:'5px', marginRight:'5px'}}, tag.name)
        }
    }

    function renderAddNewTagCmp() {
        if (createNewTagMode) {
            return re(EditTagCmp, {
                tagId: null,
                tagName: '',
                tagNameTextFieldLabel:'New tag',
                onSaved: () => {
                    setCreateNewTagMode(false)
                    reloadAllTags()
                },
                onCanceled: () => setCreateNewTagMode(false)
            })
        } else {
            return iconButton({iconName: 'add_circle', onClick: () => setCreateNewTagMode(true)})
        }
    }

    function renderPageContent() {
        if (errorLoadingTags) {
            return RE.Fragment({},
                `An error occurred during loading of tags: [${errorLoadingTags.code}] - ${errorLoadingTags.msg}`,
            )
        } else if (hasNoValue(allTags)) {
            return 'Loading tags...'
        } else {
            return RE.Container.col.top.left({style: {marginTop:'5px'}},{},
                renderAddNewTagCmp(),
                renderListOfTags()
            )
        }
    }

    return RE.Fragment({},
        renderPageContent(),
        renderMessagePopup()
    )
}
