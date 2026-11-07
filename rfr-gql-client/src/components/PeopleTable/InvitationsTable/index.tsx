import {useState} from "react";
import {PeopleTable} from "..";
import {useGetInvitations} from "../../../hooks/useGetInvitations";
import {QueryErrorAlert} from "../../QueryErrorAlert";

export const InvitationsTable = () => {
    const [page, setPage] = useState(0);
    const [search, setSearch] = useState("");

    const handleInputSearch = (value: string) => {
        setSearch(value);
        setPage(0);
    }

    const {data, loading, error, hasNextPage, hasPreviousPage, refetch} = useGetInvitations({page, search});

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
            setSearch={handleInputSearch}
            onSearchSubmit={onSearchSubmit}
        />
        </>
    )
}