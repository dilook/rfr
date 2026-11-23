import {useState} from "react";
import {PeopleTable} from ".."
import {useQueryPeople} from "../../../hooks/useQueryPeople";
import {QueryErrorAlert} from "../../QueryErrorAlert";

export const AllTable = () => {
    const [page, setPage] = useState(0);
    const [search, setSearch] = useState("");

    const handleInputSearch = (value: string) => {
        setSearch(value);
        setPage(0);
    }

    const {data, loading, error, hasNextPage, hasPreviousPage, refetch} = useQueryPeople({page, search});

    const onSearchSubmit = () => {
        refetch();
    }

    return (
        <>
        <QueryErrorAlert error={error} onRetry={() => refetch()}/>
        <PeopleTable
            data={data}
            loading={loading}
            page={page}
            setPage={setPage}
            hasNextPage={hasNextPage}
            hasPreviousPage={hasPreviousPage}
            onSearchSubmit={onSearchSubmit}
            setSearch={handleInputSearch}
        />
        </>
    )
}