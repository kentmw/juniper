import React, { useEffect, useState } from 'react'
import _keyBy from 'lodash/keyBy'
import _mapValues from 'lodash/mapValues'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from '@fortawesome/free-solid-svg-icons'
import { Link } from 'react-router-dom'
import {
  ColumnDef,
  ColumnFiltersState,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  useReactTable,
  VisibilityState
} from '@tanstack/react-table'

import Api, { Enrollee } from 'api/api'
import { StudyEnvContextT } from 'study/StudyEnvironmentRouter'
import { basicTableLayout, ColumnVisibilityControl, IndeterminateCheckbox } from 'util/tableUtils'
import LoadingSpinner from 'util/LoadingSpinner'
import { instantToDateString } from 'util/timeUtils'
import RequestKitModal from '../participants/RequestKitModal'

type EnrolleeRow = Enrollee & {
  taskCompletionStatus: Record<string, boolean>
}

/**
 * Interface for filtering/selecting enrollees who should receive sample kits.
 */
export default function KitEnrolleeSelection({ studyEnvContext }: { studyEnvContext: StudyEnvContextT }) {
  const { portal, study, currentEnv, currentEnvPath } = studyEnvContext
  const [isLoading, setIsLoading] = useState(true)
  const [enrollees, setEnrollees] = useState<EnrolleeRow[]>([])
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>({})
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({})
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [showRequestKitModal, setShowRequestKitModal] = useState(false)

  const loadEnrollees = async () => {
    setIsLoading(true)
    const enrollees = await Api.fetchEnrolleesWithKits(
      portal.shortcode, study.shortcode, currentEnv.environmentName)
    const enrolleeRows = enrollees.map(enrollee => {
      const taskCompletionStatus = _mapValues(
        _keyBy(enrollee.participantTasks, task => task.targetStableId),
        task => task.status === 'COMPLETE'
      )

      return { ...enrollee, taskCompletionStatus }
    })
    setEnrollees(enrolleeRows)
    setIsLoading(false)
  }

  useEffect(() => {
    loadEnrollees()
  }, [])

  const onSubmit = async (kitType: string) => {
    const enrolleesSelected = Object.keys(rowSelection)
      .filter(key => rowSelection[key])
      .map(key => enrollees[parseInt(key)].shortcode)

    // This iteration should be happening server-side: JN-460
    for (const shortcode of enrolleesSelected) {
      await Api.createKitRequest(
        portal.shortcode, study.shortcode, currentEnv.environmentName, shortcode, kitType)
    }

    setShowRequestKitModal(false)
    loadEnrollees()
  }

  const numSelected = Object.keys(rowSelection).length
  const enableActionButtons = numSelected > 0

  const studyColumns: ColumnDef<EnrolleeRow, string | boolean>[] = currentEnv.configuredSurveys.length === 0 ? [] :
    currentEnv.configuredSurveys
      .map(configuredSurvey => {
        const survey = configuredSurvey.survey
        return {
          id: survey.stableId,
          header: survey.name,
          accessorFn: enrollee => enrollee.taskCompletionStatus[survey.stableId],
          meta: {
            columnType: 'boolean',
            filterOptions: [
              { value: true, label: 'Completed' },
              { value: false, label: 'Not Completed' }
            ]
          },
          filterFn: 'equals',
          cell: ({ row: { original: enrollee } }) => enrollee.taskCompletionStatus[survey.stableId]
            ? <FontAwesomeIcon icon={faCheck}/>
            : ''
        }
      })

  const columns: ColumnDef<EnrolleeRow, string | boolean>[] = [{
    id: 'select',
    header: ({ table }) => <IndeterminateCheckbox
      checked={table.getIsAllRowsSelected()} indeterminate={table.getIsSomeRowsSelected()}
      onChange={table.getToggleAllRowsSelectedHandler()}/>,
    cell: ({ row }) => (
      <div className="px-1">
        <IndeterminateCheckbox
          checked={row.getIsSelected()} indeterminate={row.getIsSomeSelected()}
          onChange={row.getToggleSelectedHandler()} disabled={!row.getCanSelect()}/>
      </div>
    )
  }, {
    header: 'Enrollee shortcode',
    accessorKey: 'shortcode',
    meta: {
      columnType: 'string'
    },
    cell: data => <Link to={`${currentEnvPath}/participants/${data.getValue()}`}>{data.getValue()}</Link>
  }, {
    header: 'Join date',
    accessorKey: 'createdAt',
    cell: data => instantToDateString(Number(data.getValue()))
  }, {
    header: 'Consented',
    accessorKey: 'consented',
    meta: {
      columnType: 'boolean',
      filterOptions: [
        { value: true, label: 'Consented' },
        { value: false, label: 'Not Consented' }
      ]
    },
    filterFn: 'equals',
    cell: data => data.getValue() ? <FontAwesomeIcon icon={faCheck}/> : ''
  },
  ...studyColumns, {
    header: 'Kit requested',
    accessorFn: enrollee => enrollee.kitRequests.length !== 0,
    meta: {
      columnType: 'boolean',
      filterOptions: [
        { value: true, label: 'Requested' },
        { value: false, label: 'Not Requested' }
      ]
    },
    filterFn: 'equals',
    cell: ({ row: { original: enrollee } }) => enrollee.kitRequests.length !== 0
      ? <FontAwesomeIcon icon={faCheck}/>
      : ''
  }]

  const table = useReactTable({
    data: enrollees,
    columns,
    state: { columnVisibility, rowSelection, columnFilters },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel()
  })

  return <>
    <LoadingSpinner isLoading={isLoading}>
      <div className="d-flex align-items-center justify-content-between">
        <div className="d-flex align-items-center">
          <span className="me-2">
            {numSelected} of{' '}
            {table.getPreFilteredRowModel().rows.length} selected
          </span>
          <span className="me-2">
            <button className="btn btn-secondary"
              onClick={() => { setShowRequestKitModal(true) }}
              title={enableActionButtons ? 'Send sample collection kit' : 'Select at least one participant'}
              aria-disabled={!enableActionButtons}>Send sample collection kit</button>
          </span>
          { showRequestKitModal && <RequestKitModal
            studyEnvContext={studyEnvContext}
            onDismiss={() => setShowRequestKitModal(false)}
            onSubmit={onSubmit}/> }
        </div>
        <ColumnVisibilityControl table={table}/>
      </div>
      { basicTableLayout(table, { filterable: true }) }
      { enrollees.length === 0 && <span className="text-muted fst-italic">No participants</span> }
    </LoadingSpinner>
  </>
}