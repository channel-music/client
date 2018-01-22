'use strict'

const electron = require('electron')
const {app, BrowserWindow} = electron

let mainWindow

app.on('ready', () => {
  mainWindow = new BrowserWindow({
    width: 1024,
    height: 768
  })

  mainWindow.loadURL(`file://${__dirname}/index.html`)
  mainWindow.webContents.openDevTools()

  mainWindow.on('closed', () => { mainWindow = null })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') { app.quit() }
})

app.on('activate', () => {
  if (mainWindow === null) { createWindow() }
})
