"use strict";

function useFuncRef(func) {
    const funcRef = useRef(null)
    funcRef.current = func
    return args => funcRef.current(args)
}
