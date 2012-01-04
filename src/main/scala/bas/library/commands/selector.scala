package bas.library.commands

import bas.store.AMV

trait AMVSelector {
    def get: List[AMV] = {
        Nil
    }
}