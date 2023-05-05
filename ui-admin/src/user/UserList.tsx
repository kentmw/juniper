import React, { useEffect, useMemo, useState } from 'react'
import {
  ColumnDef,
  getCoreRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable
} from '@tanstack/react-table'
import Api, { AdminUser, Portal, PortalAdminUser } from 'api/api'
import { basicTableLayout } from 'util/tableUtils'
import { instantToDefaultString } from 'util/timeUtils'
import LoadingSpinner from 'util/LoadingSpinner'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck, faPlus } from '@fortawesome/free-solid-svg-icons'
import CreateUserModal from './CreateUserModal'

const UserList = () => {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [sorting, setSorting] = React.useState<SortingState>([])
  const [portals, setPortals] = React.useState<Portal[]>([])

  const portalColumn = (portalAdminUsers?: PortalAdminUser[]) => {
    if (!portalAdminUsers) {
      return ''
    }
    return portalAdminUsers.map(portalAdminUser =>
      portals.find(portal => portal.id === portalAdminUser.portalId)?.shortcode ?? '')
  }

  const columns: ColumnDef<AdminUser>[] = useMemo(() => ([{
    header: 'Username',
    accessorKey: 'username'
  }, {
    header: 'Superuser',
    accessorKey: 'superuser',
    cell: info => info.getValue() ? <FontAwesomeIcon icon={faCheck}/> : ''
  }, {
    header: 'Portals',
    accessorKey: 'portalAdminUsers',
    cell: info => portalColumn(info.getValue() as PortalAdminUser[])
  }, {
    header: 'Created',
    accessorKey: 'createdAt',
    cell: info => instantToDefaultString(info.getValue() as number)
  }, {
    header: 'Last login',
    accessorKey: 'lastLogin',
    cell: info => instantToDefaultString(info.getValue() as number)
  }]), [users])

  const table = useReactTable({
    data: users,
    columns,
    state: {
      sorting
    },
    enableRowSelection: true,
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel()
  })

  const loadAdminUsersAndPortals = async () => {
    setIsLoading(true)
    try {
      const result = await Promise.all(
        [Api.fetchAdminUsers(), Api.getPortals()]
      )
      setUsers(result[0])
      setPortals(result[1])
    } catch (e) {
      alert(`error loading user list ${e}`)
    }
    setIsLoading(false)
  }

  const handleUserCreated = () => {
    // just reload everything
    loadAdminUsersAndPortals()
  }

  useEffect(() => {
    loadAdminUsersAndPortals()
  }, [])
  return <div className="container p-3">
    <h1 className="h4">All users </h1>
    <button className="btn-secondary btn" onClick={() => setShowCreateModal(true)}>
      <FontAwesomeIcon icon={faPlus}/> Create user
    </button>
    {showCreateModal && <CreateUserModal onDismiss={() => setShowCreateModal(false)} portals={portals}
      userCreated={handleUserCreated}/>}
    <LoadingSpinner isLoading={isLoading}>
      {basicTableLayout(table)}
    </LoadingSpinner>
  </div>
}

export default UserList