// karma.conf.js
module.exports = (config) => {
  config.set({
    browsers: ['Chrome_without_gpu'],
    customLaunchers: {
      Chrome_without_gpu: {
        base: 'Chrome',
        flags: ['--disable-gpu', '--disable-software-rasterizer'],
      },
    },
  })
}
