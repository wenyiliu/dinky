/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


import {Button, Divider, Dropdown, Empty, Input, Menu, message, Space, Table, Tooltip} from "antd";
import {StateType} from "@/pages/DataStudio/model";
import {connect} from "umi";
import React, {useState} from "react";
import {
  CommentOutlined,
  DeleteOutlined,
  DownOutlined,
  PlusOutlined,
  PoweroffOutlined,
  ReloadOutlined,
  SearchOutlined
} from '@ant-design/icons';
import {
  changeSession,
  clearSession,
  createSession,
  listSession,
  quitSession,
  removeTable,
  showTables
} from "@/components/Studio/StudioEvent/DDL";
import {ModalForm,} from '@ant-design/pro-form';
import ProDescriptions from '@ant-design/pro-descriptions';
import SessionForm from "@/components/Studio/StudioLeftTool/StudioConnector/components/SessionForm";
import {Scrollbars} from 'react-custom-scrollbars';
import {l} from "@/utils/intl";

const StudioConnector = (props: any) => {

  const {current, toolHeight, dispatch, currentSession, session} = props;
  const [tableData, setTableData] = useState<[]>([]);
  const [loadings, setLoadings] = useState<boolean[]>([]);
  const [searchText, setSearchText] = useState<string>('');
  const [searchedColumn, setSearchedColumn] = useState<string>('');
  const [modalVisit, setModalVisit] = useState(false);
  const [type, setType] = useState<number>();
  const [sessionData, setSessionData] = useState<{}>();
  const [createSessionModalVisible, handleCreateSessionModalVisible] = useState<boolean>(false);


  const getColumnSearchProps = (dIndex) => ({
    filterDropdown: ({setSelectedKeys, selectedKeys, confirm, clearFilters}) => (
      <div style={{padding: 8}}>
        <Input
          placeholder={`Search ${dIndex}`}
          value={selectedKeys[0]}
          onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
          onPressEnter={() => handleSearch(selectedKeys, confirm, dIndex)}
          style={{marginBottom: 8, display: 'block'}}
        />
        <Space>
          <Button
            type="primary"
            onClick={() => handleSearch(selectedKeys, confirm, dIndex)}
            icon={<SearchOutlined/>}
            size="small"
            style={{width: 90}}
          >
            ??????
          </Button>
          <Button onClick={() => handleReset(clearFilters)} size="small" style={{width: 90}}>
            ??????
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setSearchText(selectedKeys[0]);
              setSearchedColumn(dIndex);
            }}
          >
            ??????
          </Button>
        </Space>
      </div>
    ),
    filterIcon: filtered => <SearchOutlined style={{color: filtered ? '#1890ff' : undefined}}/>,
    onFilter: (value, record) =>
      record[dIndex]
        ? record[dIndex].toString().toLowerCase().includes(value.toLowerCase())
        : '',
    /*render: text =>
      searchedColumn === dIndex ? (
        <Highlighter
          highlightStyle={{ backgroundColor: '#ffc069', padding: 0 }}
          searchWords={[searchText]}
          autoEscape
          textToHighlight={text ? text.toString() : ''}
        />
      ) : (
        text
      ),*/
  });

  const handleSearch = (selectedKeys, confirm, dIndex) => {
    confirm();
    setSearchText(selectedKeys[0]);
    setSearchedColumn(dIndex);
  };

  const handleReset = (clearFilters) => {
    clearFilters();
    setSearchText('');
  };

  const MoreBtn: React.FC<{
    item: any
  }> = ({item}) => (
    <Dropdown
      overlay={
        <Menu onClick={({key}) => keyEvent(key, item)}>
          <Menu.Item key="desc">??????</Menu.Item>
          <Menu.Item key="delete">{l('button.delete')}</Menu.Item>
        </Menu>
      }
    >
      <a>
        {l('button.more')} <DownOutlined/>
      </a>
    </Dropdown>
  );

  const keyEvent = (key, item) => {
    if (key == 'delete') {
      removeTable(item.tablename, currentSession.session, dispatch);
    } else {
      message.warn(l('global.stay.tuned'));
    }
  };

  const keySessionsEvent = (key, item) => {
    if (key == 'delete') {
      clearSession(item.session, dispatch);
    } else if (key == 'connect') {
      changeSession(item, dispatch);
      message.success('?????????????????????' + item.session + '????????????');
      setModalVisit(false);
    } else {
      message.warn(l('global.stay.tuned'));
    }
  };

  const getTables = () => {
    showTables(currentSession.session, dispatch);
  };

  const onClearSession = () => {
    clearSession(currentSession.session, dispatch);
  };

  const getColumns = () => {
    let columns: any = [{
      title: "??????",
      dataIndex: "table name",
      key: "table name",
      sorter: true,
      ...getColumnSearchProps("table name"),
    }, {
      title: l('global.table.operate'),
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => [
        <a
          onClick={() => {
            message.warn(l('global.stay.tuned'));
          }}
        >
          ??????
        </a>, <Divider type="vertical"/>, <a
          onClick={() => {
            keyEvent('delete', record);
          }}
        >
          {l('button.delete')}
        </a>
      ],
    },];
    return columns;
  };

  const getSessionsColumns = () => {
    let columns: any = [{
      title: "?????? Key",
      dataIndex: "session",
      key: "session",
      sorter: true,
      ...getColumnSearchProps("session"),
    }, {
      title: l('global.table.runmode'),
      key: "useRemote",
      sorter: true,
      ...getColumnSearchProps("useRemote"),
      render: function (text, record, index) {
        return record.sessionConfig.useRemote ? l('global.table.runmode.remote') : l('global.table.runmode.local') ;
      }
    }, {
      title: "?????????",
      key: "clusterName",
      sorter: true,
      ...getColumnSearchProps("clusterName"),
      render: function (text, record, index) {
        return record.sessionConfig.clusterName;
      }
    }, {
      title: "?????????",
      dataIndex: "createUser",
      key: "createUser",
      sorter: true,
      ...getColumnSearchProps("createUser"),
    }, {
      title: l('global.table.createTime'),
      dataIndex: "createTime",
      key: "createTime",
      sorter: true,
    }, {
      title: l('global.table.operate'),
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => [
        <a
          onClick={() => {
            keySessionsEvent('connect', record);
          }}
        >
          ??????
        </a>, <Divider type="vertical"/>, <a
          onClick={() => {
            keySessionsEvent('delete', record);
          }}
        >
          {l('button.delete')}
        </a>
      ],
    },];
    return columns;
  };

  const createSessions = () => {
    handleCreateSessionModalVisible(true);
  };

  const showSessions = () => {
    setModalVisit(true);
    setType(1);
    listSession(dispatch);
  };

  const quitSessions = () => {
    quitSession(dispatch);
    message.success('???????????????????????????');
  };

  return (
    <>
      <Tooltip title="????????????">
        <Button
          type="text"
          icon={<PlusOutlined/>}
          onClick={createSessions}
        />
      </Tooltip>
      {session.length > 0 ? (
        <Tooltip title="????????????">
          <Button
            type="text"
            icon={<CommentOutlined/>}
            onClick={showSessions}
          />
        </Tooltip>
      ) : ''}
      {currentSession.session && (
        <>
          <Tooltip title="????????????">
            <Button
              type="text"
              icon={<PoweroffOutlined/>}
              onClick={quitSessions}
            />
          </Tooltip>
          <Tooltip title="???????????????">
            <Button
              type="text"
              icon={<ReloadOutlined/>}
              onClick={getTables}
            />
          </Tooltip>
          <Tooltip title="????????????">
            <Button
              type="text"
              icon={<DeleteOutlined/>}
              onClick={onClearSession}
            />
          </Tooltip>
        </>)}
      <Scrollbars style={{height: (toolHeight - 32)}}>
        {currentSession.connectors && currentSession.connectors.length > 0 ? (
          <Table
            dataSource={currentSession.connectors}
            pagination={{
              defaultPageSize: 10,
              showSizeChanger: true,
            }}
            columns={getColumns()} size="small"/>) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}/>)}
        <ModalForm
          visible={modalVisit}
          onFinish={async () => {
            setSessionData(undefined);
          }}
          onVisibleChange={setModalVisit}
          submitter={{
            submitButtonProps: {
              style: {
                display: 'none',
              },
            },
          }}
        >
          {type == 1 &&
            (<ProDescriptions
                column={2}
                title='??????????????????'
              >
                <ProDescriptions.Item span={2}>
                  {session.length > 0 ?
                    (<Table dataSource={session} columns={getSessionsColumns()} size="small"
                    />) : (<Empty image={Empty.PRESENTED_IMAGE_SIMPLE}/>)}
                </ProDescriptions.Item>
              </ProDescriptions>
            )
          }
        </ModalForm>
        <SessionForm
          onSubmit={async (value) => {
            createSession(value, dispatch);
            handleCreateSessionModalVisible(false);
          }}
          onCancel={() => {
            handleCreateSessionModalVisible(false);
          }}
          updateModalVisible={createSessionModalVisible}
          values={{
            session: '',
            type: 'PUBLIC',
            useRemote: true,
          }}
        />
      </Scrollbars>
    </>
  );
};

export default connect(({Studio}: { Studio: StateType }) => ({
  current: Studio.current,
  currentSession: Studio.currentSession,
  session: Studio.session,
  toolHeight: Studio.toolHeight,
}))(StudioConnector);
