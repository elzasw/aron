/* eslint-env node */

/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/explicit-function-return-type */

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const PnpWebpackPlugin = require('pnp-webpack-plugin');
const ForkTsCheckerWebpackPlugin = require(`fork-ts-checker-webpack-plugin`);
const webpack = require('webpack');
const CopyPlugin = require('copy-webpack-plugin');

function externalForMaterialUi(context, request, callback) {
  if (/@material-ui\/core.*/.test(request)) {
    const name = request.replace(/^.*[\\\/]/, '');
    if (name === 'styles' || name === 'utils') {
      return callback(null, 'root MaterialUI');
    } else {
      return callback(null, 'root MaterialUI.' + name);
    }
  }
  callback();
}

class WatchRunPlugin {
  apply(compiler) {
    compiler.hooks.watchRun.tap('WatchRun', (comp) => {
      const changedTimes = comp.watchFileSystem.watcher.mtimes;
      const changedFiles = Object.keys(changedTimes)
        .map((file) => `\n  ${file}`)
        .join('');
      if (changedFiles.length) {
        console.log('====================================');
        console.log('NEW BUILD FILES CHANGED:', changedFiles);
        console.log('====================================');
      }
    });
  }
}

module.exports = (env, argv) => {
  // Only start `fork-ts-checker-webpack-plugin` in development mode. The plugin
  // watches for file changes and performs type checking a separate thread to
  // avoid interfering with code transpilation. In a production build, type
  // checking is performed by calling `tsc --noEmit` once before webpack build
  // begins.
  const modeDependentPlugins =
    argv.mode === 'development'
      ? [
          new ForkTsCheckerWebpackPlugin({
            checkSyntacticErrors: true,
          }),
          new WatchRunPlugin(),
        ]
      : [];

  const urlPrefix = argv.urlPrefix ? `/${argv.urlPrefix}` : '';

  return {
    entry: './src/index.tsx',
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: argv.mode === 'development' ? '[name].js' : '[name].[hash].js',
      publicPath: `${urlPrefix}/`,
    },
    watchOptions: {
      // for Windows
      // ignored: ['**/node_modules/**', '**/build/common-web/**'],
      aggregateTimeout: 3000,
      poll: 5000,
    },
    externals: [
      {
        react: 'React',
        'react-dom': 'ReactDOM',
        'react-router': 'ReactRouter',
        'react-router-dom': 'ReactRouterDOM',
        lodash: '_',
        'react-intl': 'ReactIntl',
      },
      externalForMaterialUi,
    ],
    module: {
      rules: [
        {
          // Use babel with @babel/preset-typescript to transpile TypeScript
          // instead of ts-loader
          test: /\.tsx?$/,
          exclude: /node_modules/,
          use: require.resolve('babel-loader'),
        },
        {
          // `css-loader` converts a static CSS file into a Common JS module and
          // `style-loader` injects the CSS into a `style` tag in the
          // `document`'s `head`:
          test: /\.css$/,
          use: ['style-loader', 'css-loader'],
        },
        {
          test: /\.(png|jpe?g|gif|svg)$/i,
          use: [
            {
              loader: 'file-loader',
              options: {
                outputPath: 'images',
              },
            },
          ],
        },
      ],
    },
    resolve: {
      extensions: ['.ts', '.tsx', '.js', '.jsx'],
      plugins: [
        // This is necessary to make webpack work with Yarn PnP:
        PnpWebpackPlugin,
      ],
    },

    // This source map option allows us to see the code before transpilation,
    // just as it was authored. All modules are separated from each other:
    devtool: 'source-map',

    // Do not show bundle information except when error happens:
    stats: 'minimal',

    plugins: [
      ...modeDependentPlugins,
      new CopyPlugin({
        patterns: [
          {
            from: path.resolve(
              __dirname,
              'public',
              'hoist-non-react-statics.umd.js'
            ),
            to: 'hoist-non-react-statics.umd.js',
          },
          { from: path.resolve(__dirname, 'public', '.htaccess'), to: '.' },
          { from: path.resolve(__dirname, 'public', 'configuration.js'), to: '.' },
        ],
      }),
      new HtmlWebpackPlugin({
        template: 'src/template.html',
      }),
      new webpack.DefinePlugin({
        // Pass `API_URL` environment variable, defined in `.env`:
        'process.env.API_URL': JSON.stringify(process.env.API_URL),
        'process.env.NODE_ENV': JSON.stringify(argv.mode || 'development'),
        'process.env.URL_PREFIX': JSON.stringify(urlPrefix),
      }),
    ],
    resolveLoader: {
      plugins: [
        // This is necessary to make webpack work with Yarn PnP:
        PnpWebpackPlugin.moduleLoader(module),
      ],
    },
  };
};
