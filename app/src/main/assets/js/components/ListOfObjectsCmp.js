"use strict";

const ListOfObjectsCmp = ({objects,beginIdx,endIdx,onObjectClicked,renderObject}) => {

    return RE.table({},
        RE.tbody({},
            objects.filter((obj,idx) => beginIdx <= idx && idx <= endIdx).map(obj =>
                RE.tr(
                    {
                        key:obj.id,
                        onClick: () => onObjectClicked(obj.id),
                    },
                    RE.td({}, RE.Paper({},renderObject(obj))),
                )
            )
        )
    )

}